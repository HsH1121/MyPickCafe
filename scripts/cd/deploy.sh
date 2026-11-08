#!/usr/bin/env bash
# MyPickCafe EC2 배포 스크립트
#
# GitHub Actions 의 Deploy 워크플로가 SSM(AWS-RunShellScript)으로 서버에서 root 로 실행합니다.
# 서버 저장소에 있는 파일이 아니라, 배포할 커밋의 이 파일을 git show 로 꺼내 실행합니다.
#
#   사용: APP_DIR=/home/ubuntu/MyPickCafe deploy.sh <커밋 SHA 40자> <web 이미지@sha256:...> <ai 이미지@sha256:...>
#
# 순서 (앞 단계가 끝나기 전에는 실행 중인 서비스를 건드리지 않습니다)
#   1. 사전 점검: 잠금, 커밋이 origin/main 에 있는지, 서버 작업 트리, 필수 볼륨과 소유자, postgres 상태
#   2. 두 이미지를 digest 로 pull
#   3. 서버 저장소를 그 커밋으로 checkout 하고, 이미지 override 를 붙여 compose 설정 검사
#      (실패하면 checkout 을 원래대로 돌려 놓고 끝냅니다)
#   4. web·ai 컨테이너만 교체합니다. postgres·ollama·caddy 는 그대로 둡니다.
#   5. 두 컨테이너 healthy, 실행 중인 이미지, web HTTP 응답, AI 인덱스 건수 확인
#
# 하지 않는 일: compose down, 볼륨 삭제·prune, DB 초기화·스키마 변경, Chroma 초기화·재인덱싱,
#   볼륨 chown, 실패 시 자동 되돌리기(되돌릴 때는 Deploy 워크플로를 이전 성공 SHA 로 다시 실행).
#
# 이 스크립트의 출력은 공개 저장소의 Actions 로그에 그대로 남습니다.
# 비밀값과 앱 로그는 출력하지 않고, 실패했을 때의 컨테이너 로그는 서버의 $STATE_DIR/last-failure.log 에만 남깁니다.
set -euo pipefail
umask 077

COMPOSE_PROJECT=mypickcafe
WEB_SERVICE=mypickcafe-web
AI_SERVICE=mypickcafe-ai
PG_CONTAINER=mypickcafe-postgres
# 이미지 Dockerfile 의 appuser uid. 볼륨이 이 uid 소유가 아니면 앱이 쓰지 못합니다.
APP_UID=10001

STATE_DIR=${STATE_DIR:-/var/lib/mypickcafe-deploy}
# 없으면 배포하지 않는 볼륨. 없는 채로 띄우면 compose 가 빈 볼륨을 새로 만들어 데이터가 사라진 것처럼 보입니다.
REQUIRED_VOLUMES=${REQUIRED_VOLUMES:-mypickcafe_pgdata mypickcafe_chroma-index mypickcafe_web-uploads}
# 앱(uid 10001)이 쓰는 볼륨. 있는 것만 소유자를 확인합니다(없으면 compose 가 새로 만들고 이미지의 소유자를 따릅니다).
APP_WRITABLE_VOLUMES=${APP_WRITABLE_VOLUMES:-mypickcafe_chroma-index mypickcafe_ai-logs mypickcafe_web-uploads mypickcafe_web-private}
WEB_TIMEOUT=${WEB_TIMEOUT:-300}
# 인덱스가 비어 있으면 AI 서버가 기동하면서 전체 인덱싱을 하므로 넉넉히 둡니다.
AI_TIMEOUT=${AI_TIMEOUT:-900}
POLL_INTERVAL=${POLL_INTERVAL:-5}

log() { printf '[deploy %s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
fail() {
    printf '[deploy %s] 실패: %s\n' "$(date -u +%H:%M:%S)" "$*" >&2
    exit 1
}

phase=start
previous_ref=""
last_good_sha=""
candidate=""

compose() {
    local override=$1
    shift
    docker compose --project-name "$COMPOSE_PROJECT" --project-directory "$APP_DIR" \
        -f "$APP_DIR/docker-compose.yml" -f "$override" "$@"
}

on_exit() {
    local rc=$?
    [ "$rc" -eq 0 ] && return 0
    log "중단됨: 단계 $phase, 종료 코드 $rc"
    case $phase in
        checked_out)
            # 컨테이너는 아직 그대로입니다. 서버 저장소만 원래 커밋으로 돌려 놓습니다.
            if as_owner git -C "$APP_DIR" checkout --quiet "$previous_ref"; then
                log "서버 저장소를 원래 위치($previous_ref)로 되돌렸습니다. 실행 중인 서비스는 바뀌지 않았습니다."
            else
                log "서버 저장소를 원래 위치($previous_ref)로 되돌리지 못했습니다. 서버에서 직접 확인하세요."
            fi
            ;;
        swapping | verifying)
            {
                echo "# $(date -u +%FT%TZ) 배포 실패 ($deploy_sha)"
                compose "$candidate" ps --all || true
                for svc in "$WEB_SERVICE" "$AI_SERVICE"; do
                    echo "# docker logs --tail 200 $svc"
                    docker logs --tail 200 "$svc" 2>&1 || true
                done
            } >"$STATE_DIR/last-failure.log" 2>&1 || true
            log "컨테이너 상태와 최근 로그는 서버의 $STATE_DIR/last-failure.log 에 남겼습니다(공개 로그에는 출력하지 않음)."
            log "web·ai 가 중단됐거나 새 버전이 일부만 떠 있을 수 있습니다. 자동으로 되돌리지 않습니다."
            if [ -n "$last_good_sha" ]; then
                log "되돌리기: Actions → Deploy → Run workflow 에 이전 성공 SHA $last_good_sha 를 넣어 실행하세요."
            else
                log "CD 로 배포한 기록이 아직 없어 이전 성공 SHA 가 없습니다. 서버에서 직접 되돌리려면:"
                log "  cd $APP_DIR && git checkout $previous_ref && sudo docker compose up -d --no-deps --no-build $AI_SERVICE $WEB_SERVICE"
            fi
            ;;
    esac
}
trap on_exit EXIT

# ── 인자와 환경 확인 ─────────────────────────────────────────────────────
[ $# -eq 3 ] || fail "사용법: APP_DIR=<서버 저장소> $0 <커밋 SHA> <web 이미지@digest> <ai 이미지@digest>"
deploy_sha=$1
web_image=$2
ai_image=$3
image_re='^[a-z0-9][a-z0-9._/-]*@sha256:[0-9a-f]{64}$'
[[ $deploy_sha =~ ^[0-9a-f]{40}$ ]] || fail "커밋 SHA 형식이 아닙니다: $deploy_sha"
[[ $web_image =~ $image_re ]] || fail "web 이미지는 이름@sha256:digest 형식이어야 합니다: $web_image"
[[ $ai_image =~ $image_re ]] || fail "ai 이미지는 이름@sha256:digest 형식이어야 합니다: $ai_image"

[ "$(id -u)" -eq 0 ] || fail "root 로 실행해야 합니다(SSM RunShellScript 는 root 로 실행합니다)."
[ -n "${APP_DIR:-}" ] || fail "APP_DIR(서버의 저장소 경로)가 비어 있습니다."
[ -f "$APP_DIR/docker-compose.yml" ] || fail "$APP_DIR/docker-compose.yml 이 없습니다."
[ -f "$APP_DIR/.env" ] || fail "$APP_DIR/.env 가 없습니다. 서버에서 쓰던 .env 를 그대로 씁니다."
for cmd in docker git curl flock runuser stat; do
    command -v "$cmd" >/dev/null 2>&1 || fail "$cmd 명령이 없습니다."
done
docker compose version >/dev/null 2>&1 || fail "docker compose v2 플러그인이 필요합니다."

# 서버 저장소는 보통 ubuntu 사용자 소유입니다. root 로 git 을 돌리면 파일 소유자가 바뀌므로 소유자로 실행합니다.
owner=$(stat -c %U "$APP_DIR")
as_owner() {
    if [ "$owner" = root ]; then "$@"; else runuser -u "$owner" -- "$@"; fi
}

mkdir -p "$STATE_DIR"
chmod 700 "$STATE_DIR"
exec 9>"$STATE_DIR/deploy.lock"
flock -n 9 || fail "다른 배포가 진행 중입니다($STATE_DIR/deploy.lock)."

log "배포 대상 커밋 $deploy_sha"
log "web 이미지 $web_image"
log "ai 이미지 $ai_image"

# ── 1. 사전 점검 (아무것도 바꾸지 않음) ──────────────────────────────────
phase=preflight
log "1/5 사전 점검"
as_owner git -C "$APP_DIR" merge-base --is-ancestor "$deploy_sha" origin/main \
    || fail "$deploy_sha 가 서버 저장소의 origin/main 에 없습니다(main 이 아닌 커밋이거나 fetch 전)."

dirty=$(as_owner git -C "$APP_DIR" status --porcelain --untracked-files=no)
[ -z "$dirty" ] || fail "서버 저장소에 커밋되지 않은 변경이 있습니다(git status). 정리한 뒤 다시 실행하세요."

for vol in $REQUIRED_VOLUMES; do
    docker volume inspect "$vol" >/dev/null 2>&1 \
        || fail "필수 볼륨 $vol 이 없습니다. 볼륨 이름(docker volume ls)을 확인하세요. 새 빈 볼륨으로 띄우지 않습니다."
done
for vol in $APP_WRITABLE_VOLUMES; do
    mountpoint=$(docker volume inspect -f '{{.Mountpoint}}' "$vol" 2>/dev/null) || continue
    vol_uid=$(stat -c %u "$mountpoint")
    [ "$vol_uid" = "$APP_UID" ] \
        || fail "볼륨 $vol 의 소유자가 uid $vol_uid 입니다. 앱은 uid $APP_UID 로 돌아 쓰기에 실패합니다. 확인 후 직접 chown 하세요(이 스크립트는 바꾸지 않습니다)."
done

pg_health=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$PG_CONTAINER" 2>/dev/null || true)
[ "$pg_health" = healthy ] || fail "$PG_CONTAINER 가 healthy 가 아닙니다(${pg_health:-없음}). DB 가 떠 있어야 배포합니다."

previous_ref=$(as_owner git -C "$APP_DIR" symbolic-ref -q --short HEAD || as_owner git -C "$APP_DIR" rev-parse HEAD)
if [ -f "$STATE_DIR/current" ]; then
    last_good_sha=$(sed -n 's/^sha=//p' "$STATE_DIR/current")
fi

ai_indexed() {
    docker exec "$AI_SERVICE" python -c \
        'import json, urllib.request; print(json.load(urllib.request.urlopen("http://127.0.0.1:8000/health", timeout=5))["indexed"])' \
        2>/dev/null
}
indexed_before=$(ai_indexed || true)
[[ $indexed_before =~ ^[0-9]+$ ]] || indexed_before=""
log "현재 서버 저장소 위치 $previous_ref, 이전 성공 배포 ${last_good_sha:-없음}, AI 인덱스 ${indexed_before:-확인 불가}건"

# ── 2. 이미지 pull (아직 서비스는 그대로) ────────────────────────────────
phase=pull
log "2/5 이미지 pull"
docker pull --quiet "$web_image" >/dev/null || fail "web 이미지를 받지 못했습니다(GHCR 공개 여부 또는 서버의 docker login 확인)."
docker pull --quiet "$ai_image" >/dev/null || fail "ai 이미지를 받지 못했습니다(GHCR 공개 여부 또는 서버의 docker login 확인)."

# ── 3. 배포할 커밋의 compose 로 설정 검사 ────────────────────────────────
log "3/5 $deploy_sha 의 compose 설정 검사"
candidate="$STATE_DIR/images.override.yml.candidate"
cat >"$candidate" <<EOF
# scripts/cd/deploy.sh 가 만든 파일입니다. 직접 고치지 마세요.
# 서버에서 compose 를 직접 다룰 때도 이 파일을 함께 넘겨야 배포된 이미지를 씁니다.
services:
  $AI_SERVICE:
    image: $ai_image
  $WEB_SERVICE:
    image: $web_image
EOF
phase=checked_out
as_owner git -C "$APP_DIR" checkout --quiet --detach "$deploy_sha"
# --quiet: 해석된 설정(.env 의 비밀값 포함)을 출력하지 않습니다.
compose "$candidate" config --quiet || fail "compose 설정 검사에 실패했습니다."

# ── 4. web·ai 만 교체 ────────────────────────────────────────────────────
phase=swapping
log "4/5 $AI_SERVICE·$WEB_SERVICE 교체 (postgres·ollama·caddy 는 그대로, 교체 중 잠시 접속 불가)"
compose "$candidate" up -d --no-deps --no-build "$AI_SERVICE" "$WEB_SERVICE"

# ── 5. 확인 ──────────────────────────────────────────────────────────────
phase=verifying
log "5/5 상태 확인"

wait_healthy() {
    local name=$1 timeout=$2 waited=0 status
    while :; do
        status=$(docker inspect -f '{{.State.Status}}/{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$name" 2>/dev/null || echo missing/)
        case $status in
            running/healthy)
                log "$name healthy (${waited}초)"
                return 0
                ;;
            # restart: unless-stopped 라 기동 중 죽으면 restarting 이 됩니다. 기다리지 않고 실패로 봅니다.
            restarting/* | exited/* | dead/* | missing/*)
                fail "$name 컨테이너가 정상 실행 중이 아닙니다($status)."
                ;;
        esac
        [ "$waited" -lt "$timeout" ] || fail "$name 가 ${timeout}초 안에 healthy 가 되지 않았습니다($status)."
        sleep "$POLL_INTERVAL"
        waited=$((waited + POLL_INTERVAL))
    done
}

check_running_image() {
    local name=$1 ref=$2 want got
    want=$(docker image inspect -f '{{.Id}}' "$ref")
    got=$(docker inspect -f '{{.Image}}' "$name")
    [ "$want" = "$got" ] || fail "$name 가 배포한 이미지로 실행되고 있지 않습니다."
}

env_value() {
    sed -n "s/^$1=//p" "$APP_DIR/.env" | tail -n 1 | tr -d "\"' \r"
}

wait_healthy "$AI_SERVICE" "$AI_TIMEOUT"
wait_healthy "$WEB_SERVICE" "$WEB_TIMEOUT"
check_running_image "$AI_SERVICE" "$ai_image"
check_running_image "$WEB_SERVICE" "$web_image"

web_port=$(env_value WEB_PORT)
[[ $web_port =~ ^[0-9]+$ ]] || web_port=8080
http_code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:$web_port/" || true)
[[ $http_code =~ ^[23][0-9][0-9]$ ]] || fail "web 첫 화면 응답이 정상이 아닙니다(HTTP ${http_code:-없음})."
log "web 첫 화면 HTTP $http_code"

indexed_after=$(ai_indexed || true)
[[ $indexed_after =~ ^[0-9]+$ ]] || fail "AI /health 응답에서 인덱스 건수를 읽지 못했습니다."
if [ -n "$indexed_before" ] && [ "$indexed_before" -gt 0 ]; then
    [ "$indexed_after" -ge "$indexed_before" ] \
        || fail "AI 인덱스가 ${indexed_before}건에서 ${indexed_after}건으로 줄었습니다(볼륨 연결 확인)."
else
    [ "$indexed_after" -gt 0 ] || fail "AI 인덱스가 0건입니다."
fi
log "AI 인덱스 ${indexed_after}건"

# ── 기록 ─────────────────────────────────────────────────────────────────
mv -f "$candidate" "$STATE_DIR/images.override.yml"
[ ! -f "$STATE_DIR/current" ] || cp -f "$STATE_DIR/current" "$STATE_DIR/previous"
printf 'sha=%s\nweb=%s\nai=%s\ndeployed_at=%s\n' "$deploy_sha" "$web_image" "$ai_image" "$(date -u +%FT%TZ)" \
    >"$STATE_DIR/current.tmp"
mv -f "$STATE_DIR/current.tmp" "$STATE_DIR/current"
printf '%s %s %s %s\n' "$(date -u +%FT%TZ)" "$deploy_sha" "$web_image" "$ai_image" >>"$STATE_DIR/history"
phase=finished
log "배포 완료: $deploy_sha (이전 성공 배포 ${last_good_sha:-없음})"
