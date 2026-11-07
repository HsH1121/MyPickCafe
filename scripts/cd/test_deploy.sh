#!/usr/bin/env bash
# scripts/cd/deploy.sh 를 가짜 docker·git 등으로 실행해 순서와 실패 처리를 확인합니다.
# 실제 컨테이너·저장소·네트워크는 건드리지 않습니다.
#
#   bash scripts/cd/test_deploy.sh
#
# 확인하는 것
#   - 사전 점검이나 pull 이 실패하면 checkout·컨테이너 교체를 하지 않는다
#   - compose 설정 검사가 실패하면 서버 저장소를 원래 위치로 되돌리고 교체하지 않는다
#   - 교체 뒤 확인이 실패하면 성공 기록(current)을 바꾸지 않고, 로그는 출력 대신 파일로 남긴다
#   - 성공하면 pull → checkout → 설정 검사 → 교체 순서이고 기록을 남긴다
set -uo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
DEPLOY=${DEPLOY:-"$ROOT/scripts/cd/deploy.sh"}
SHA=1111111111111111111111111111111111111111
OLD_SHA=2222222222222222222222222222222222222222
WEB_REF="ghcr.io/hsh1121/mypickcafe-web@sha256:$(printf 'a%.0s' {1..64})"
AI_REF="ghcr.io/hsh1121/mypickcafe-ai@sha256:$(printf 'b%.0s' {1..64})"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
BIN="$WORK/bin"
mkdir -p "$BIN"

# ── 가짜 명령 ────────────────────────────────────────────────────────────
# 모든 호출을 $CALLS 에 한 줄씩 남깁니다. 동작은 FAKE_* 환경변수로 바꿉니다.
cat >"$BIN/docker" <<'EOF'
#!/usr/bin/env bash
echo "docker $*" >>"$CALLS"
args="$*"
case $args in
    "compose version") exit 0 ;;
    "volume inspect -f {{.Mountpoint}} "*) echo "/fake/${args##* }" ;;
    "volume inspect "*)
        for v in $FAKE_MISSING_VOLUMES; do [ "${args##* }" = "$v" ] && exit 1; done
        exit 0 ;;
    *"mypickcafe-postgres") echo healthy ;;
    "exec mypickcafe-ai python -c "*)
        n=$(cat "$STATE/exec_count" 2>/dev/null || echo 0); echo $((n + 1)) >"$STATE/exec_count"
        if [ "$n" -eq 0 ]; then echo "$FAKE_INDEX_BEFORE"; else echo "$FAKE_INDEX_AFTER"; fi ;;
    "pull --quiet "*)
        [ "${args##* }" = "$FAKE_PULL_FAIL" ] && exit 1
        exit 0 ;;
    compose*" config --quiet") [ -n "$FAKE_CONFIG_FAIL" ] && exit 1; exit 0 ;;
    compose*" up -d "*) exit 0 ;;
    compose*" ps --all") echo "fake ps" ;;
    "inspect -f {{.State.Status}}"*mypickcafe-web) echo "$FAKE_WEB_STATUS" ;;
    "inspect -f {{.State.Status}}"*mypickcafe-ai) echo "running/healthy" ;;
    "image inspect -f {{.Id}} "*) echo "id:${args##* }" ;;
    "inspect -f {{.Image}} mypickcafe-web") echo "id:$FAKE_WEB_RUNNING" ;;
    "inspect -f {{.Image}} mypickcafe-ai") echo "id:$FAKE_AI_RUNNING" ;;
    "logs "*) echo "SECRET_APP_LOG_LINE" ;;
    *) echo "unexpected docker call: $args" >&2; exit 99 ;;
esac
EOF

cat >"$BIN/git" <<'EOF'
#!/usr/bin/env bash
echo "git $*" >>"$CALLS"
shift 2  # -C <dir>
case "$*" in
    "merge-base --is-ancestor "*) [ -z "$FAKE_NOT_ANCESTOR" ] ;;
    "status --porcelain --untracked-files=no") printf '%s' "$FAKE_DIRTY" ;;
    "symbolic-ref -q --short HEAD") echo main ;;
    "rev-parse HEAD") echo "$OLD_SHA" ;;
    "checkout --quiet "*) exit 0 ;;
    *) echo "unexpected git call: $*" >&2; exit 99 ;;
esac
EOF

cat >"$BIN/stat" <<'EOF'
#!/usr/bin/env bash
case $2 in
    %U) echo ubuntu ;;
    %u) echo "$FAKE_VOLUME_UID" ;;
esac
EOF

cat >"$BIN/runuser" <<'EOF'
#!/usr/bin/env bash
while [ "$1" != "--" ]; do shift; done
shift
exec "$@"
EOF

cat >"$BIN/curl" <<'EOF'
#!/usr/bin/env bash
echo "curl $*" >>"$CALLS"
printf '%s' "$FAKE_HTTP_CODE"
EOF

printf '#!/usr/bin/env bash\necho 0\n' >"$BIN/id"
printf '#!/usr/bin/env bash\nexit 0\n' >"$BIN/flock"
printf '#!/usr/bin/env bash\nexit 0\n' >"$BIN/sleep"
chmod +x "$BIN"/*

# ── 실행 도우미 ──────────────────────────────────────────────────────────
passed=0
failed=0

run_case() {
    CASE_DIR="$WORK/$1"
    mkdir -p "$CASE_DIR/app" "$CASE_DIR/state" "$CASE_DIR/fake"
    touch "$CASE_DIR/app/docker-compose.yml"
    printf 'WEB_PORT=8080\nJWT_SECRET=do-not-print\n' >"$CASE_DIR/app/.env"
    export CALLS="$CASE_DIR/calls" STATE="$CASE_DIR/fake" OLD_SHA
    : >"$CALLS"
    OUT=$(
        PATH="$BIN:$PATH" APP_DIR="$CASE_DIR/app" STATE_DIR="$CASE_DIR/state" \
            AI_TIMEOUT=10 WEB_TIMEOUT=10 POLL_INTERVAL=5 \
            bash "$DEPLOY" "$SHA" "$WEB_REF" "$AI_REF" 2>&1
    )
    RC=$?
}

reset_fakes() {
    export FAKE_MISSING_VOLUMES="" FAKE_VOLUME_UID=10001 FAKE_INDEX_BEFORE=100 FAKE_INDEX_AFTER=100 \
        FAKE_PULL_FAIL="" FAKE_CONFIG_FAIL="" FAKE_WEB_STATUS="running/healthy" \
        FAKE_WEB_RUNNING="$WEB_REF" FAKE_AI_RUNNING="$AI_REF" FAKE_HTTP_CODE=200 \
        FAKE_NOT_ANCESTOR="" FAKE_DIRTY=""
}

check() {
    local desc=$1
    shift
    if "$@"; then
        passed=$((passed + 1))
    else
        failed=$((failed + 1))
        echo "  FAIL: $desc"
    fi
}
called() { grep -qF -- "$1" "$CALLS"; }
not_called() { ! called "$1"; }
line_of() { grep -nF -- "$1" "$CALLS" | head -n 1 | cut -d: -f1; }
before() { [ "$(line_of "$1")" -lt "$(line_of "$2")" ]; }
output_has() { grep -qF -- "$1" <<<"$OUT"; }
output_lacks() { ! output_has "$1"; }

nothing_changed() {
    check "checkout 하지 않음" not_called "checkout --quiet --detach"
    check "컨테이너 교체하지 않음" not_called " up -d "
    check "성공 기록 없음" test ! -f "$CASE_DIR/state/current"
}

# ── 경우별 확인 ──────────────────────────────────────────────────────────
echo "[성공]"
reset_fakes
run_case success
check "종료 코드 0 ($RC)" test "$RC" -eq 0
check "web pull → ai pull" before "pull --quiet $WEB_REF" "pull --quiet $AI_REF"
check "pull 뒤에 checkout" before "pull --quiet $AI_REF" "checkout --quiet --detach $SHA"
check "checkout 뒤에 설정 검사" before "checkout --quiet --detach $SHA" "config --quiet"
check "설정 검사 뒤에 교체" before "config --quiet" " up -d "
check "web·ai 만 --no-deps --no-build 로 교체" \
    called "up -d --no-deps --no-build mypickcafe-ai mypickcafe-web"
check "프로젝트 이름 고정과 override 사용" called "--project-name mypickcafe"
check "down·볼륨 삭제·prune 없음" bash -c "! grep -qE ' down| rm | prune|volume rm' '$CALLS'"
check "성공 기록에 SHA" grep -qx "sha=$SHA" "$CASE_DIR/state/current"
check "override 에 digest 이미지" grep -qF "image: $WEB_REF" "$CASE_DIR/state/images.override.yml"
check "후보 파일 정리" test ! -f "$CASE_DIR/state/images.override.yml.candidate"
check ".env 값 출력 안 함" output_lacks "do-not-print"

echo "[이전 성공 기록이 있을 때 되돌리기 안내]"
reset_fakes
# 상태 폴더에 이전 성공 기록을 먼저 만들어 둡니다.
CASE_DIR="$WORK/with_history"
mkdir -p "$CASE_DIR/state"
printf 'sha=%s\n' "$OLD_SHA" >"$CASE_DIR/state/current"
mkdir -p "$CASE_DIR/app" "$CASE_DIR/fake"
touch "$CASE_DIR/app/docker-compose.yml"
printf 'WEB_PORT=8080\n' >"$CASE_DIR/app/.env"
export CALLS="$CASE_DIR/calls" STATE="$CASE_DIR/fake"
: >"$CALLS"
OUT=$(FAKE_WEB_STATUS="restarting/" PATH="$BIN:$PATH" APP_DIR="$CASE_DIR/app" STATE_DIR="$CASE_DIR/state" \
    AI_TIMEOUT=10 WEB_TIMEOUT=10 bash "$DEPLOY" "$SHA" "$WEB_REF" "$AI_REF" 2>&1)
RC=$?
check "실패 종료" test "$RC" -ne 0
check "이전 성공 SHA 로 되돌리기 안내" output_has "이전 성공 SHA $OLD_SHA"
check "성공 기록 유지" grep -qx "sha=$OLD_SHA" "$CASE_DIR/state/current"

echo "[필수 볼륨 없음]"
reset_fakes
FAKE_MISSING_VOLUMES="mypickcafe_chroma-index" run_case missing_volume
check "실패 종료" test "$RC" -ne 0
check "이유 출력" output_has "필수 볼륨 mypickcafe_chroma-index"
check "pull 하지 않음" not_called "pull --quiet"
nothing_changed

echo "[볼륨 소유자가 앱 uid 가 아님]"
reset_fakes
FAKE_VOLUME_UID=0 run_case volume_owner
check "실패 종료" test "$RC" -ne 0
check "이유 출력" output_has "소유자가 uid 0"
check "pull 하지 않음" not_called "pull --quiet"
nothing_changed

echo "[main 에 없는 커밋]"
reset_fakes
FAKE_NOT_ANCESTOR=1 run_case not_on_main
check "실패 종료" test "$RC" -ne 0
check "pull 하지 않음" not_called "pull --quiet"
nothing_changed

echo "[서버 저장소에 수정된 파일]"
reset_fakes
FAKE_DIRTY=" M docker-compose.yml" run_case dirty_tree
check "실패 종료" test "$RC" -ne 0
nothing_changed

echo "[ai 이미지 pull 실패]"
reset_fakes
FAKE_PULL_FAIL="$AI_REF" run_case pull_fail
check "실패 종료" test "$RC" -ne 0
nothing_changed

echo "[compose 설정 검사 실패]"
reset_fakes
FAKE_CONFIG_FAIL=1 run_case config_fail
check "실패 종료" test "$RC" -ne 0
check "원래 브랜치로 checkout 복구" called "checkout --quiet main"
check "컨테이너 교체하지 않음" not_called " up -d "
check "성공 기록 없음" test ! -f "$CASE_DIR/state/current"

echo "[교체 뒤 web 이 재시작 반복]"
reset_fakes
FAKE_WEB_STATUS="restarting/" run_case web_restarting
check "실패 종료" test "$RC" -ne 0
check "성공 기록 없음" test ! -f "$CASE_DIR/state/current"
check "override 승격 안 함" test ! -f "$CASE_DIR/state/images.override.yml"
check "실패 로그 파일 남김" grep -qF "SECRET_APP_LOG_LINE" "$CASE_DIR/state/last-failure.log"
check "앱 로그는 출력하지 않음" output_lacks "SECRET_APP_LOG_LINE"
check "기록이 없을 때 서버 직접 복구 안내" output_has "git checkout main"

echo "[다른 이미지로 실행 중]"
reset_fakes
FAKE_WEB_RUNNING="old-image" run_case wrong_image
check "실패 종료" test "$RC" -ne 0
check "성공 기록 없음" test ! -f "$CASE_DIR/state/current"

echo "[web HTTP 오류]"
reset_fakes
FAKE_HTTP_CODE=502 run_case http_fail
check "실패 종료" test "$RC" -ne 0
check "이유 출력" output_has "HTTP 502"

echo "[AI 인덱스 감소]"
reset_fakes
FAKE_INDEX_AFTER=0 run_case index_drop
check "실패 종료" test "$RC" -ne 0
check "이유 출력" output_has "100건에서 0건"
check "성공 기록 없음" test ! -f "$CASE_DIR/state/current"

echo "[잘못된 인자]"
reset_fakes
CASE_DIR="$WORK/bad_args"
mkdir -p "$CASE_DIR"
export CALLS="$CASE_DIR/calls"
: >"$CALLS"
OUT=$(PATH="$BIN:$PATH" APP_DIR=/nowhere STATE_DIR="$CASE_DIR/state" \
    bash "$DEPLOY" "$SHA" "mypickcafe-web:latest" "$AI_REF" 2>&1)
RC=$?
check "digest 없는 이미지 거부" test "$RC" -ne 0
check "아무 명령도 실행 안 함" test ! -s "$CALLS"

echo
echo "통과 $passed, 실패 $failed"
[ "$failed" -eq 0 ]
