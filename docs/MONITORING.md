# MyPickCafe 모니터링·부하 테스트

Spring Boot 앱의 지표를 **Prometheus** 가 모으고 **Grafana** 로 보며, **k6** 로 부하를 줘서 어디까지 버티는지 숫자로 확인한다.
모두 배포에 쓰는 `docker-compose.yml` 안에 프로필로 들어 있어, 부하 테스트는 운영과 같은 이미지·메모리 한도·prod 프로파일의 컨테이너를 대상으로 한다.

| 프로필 | 서비스 | 기본 `docker compose up` | 배포(`deploy.sh`) |
| --- | --- | --- | --- |
| `monitoring` | prometheus, grafana | 뜨지 않음 | 건드리지 않음 |
| `k6` | k6 (테스트할 때만 `run --rm`) | 뜨지 않음 | 건드리지 않음 |

## 구성

```mermaid
flowchart LR
    K[k6] -->|부하 :8080| W[mypickcafe-web]
    K -->|결과 remote write| P[Prometheus :9090]
    P -->|15초마다 수집 :8081/actuator/prometheus| W
    G[Grafana :3000] -->|PromQL| P
    W --> D[(PostgreSQL)]
```

| 파일 | 역할 |
| --- | --- |
| `MyPickCafe_Springboot/build.gradle` | `spring-boot-starter-actuator`, `micrometer-registry-prometheus` |
| `MyPickCafe_Springboot/.../application.properties` | 관리 포트 8081, `health`·`prometheus` 만 노출, 응답 시간 histogram, Tomcat 스레드 지표 |
| `MyPickCafe_Springboot/.../SecurityConfig.java` | `/actuator/health`, `/actuator/prometheus` 만 인증 없이 허용 |
| `.../security/ActuatorExposureTest.java` | 관리 포트에서만 열리는지, 다른 엔드포인트는 막혔는지, 대시보드가 쓰는 지표가 나오는지 |
| `docker-compose.yml` | `prometheus`·`grafana`(monitoring), `k6`(k6), `monitoring` 네트워크 |
| `monitoring/prometheus/prometheus.yml` | 수집 대상(`mypickcafe-web:8081`) |
| `monitoring/prometheus/alert-rules.yml` | 알림 규칙 5개 |
| `monitoring/grafana/provisioning/` | 데이터 소스·대시보드 자동 등록 |
| `monitoring/grafana/dashboards/` | `MyPickCafe 웹 서버`, `k6 부하 테스트` 대시보드 |
| `monitoring/k6/` | `smoke.js`, `load.js`, `stress.js`, `slow-ai.js`, 공통 흐름 `lib/mypickcafe.js` |
| `monitoring/ai-stub/` | 느린 AI 테스트용 가짜 AI 서버와 덧붙임 compose 설정(아래 "느린 AI 테스트") |

### 지표를 밖에 내보내지 않는 방법

- Actuator 는 **관리 포트 8081** 에서만 열린다. Caddy 는 8080 만 프록시하고, compose 는 8081 을 호스트에 열지 않는다.
  `monitoring` 네트워크에 붙은 Prometheus 만 가져갈 수 있다. 메인 포트의 `/actuator/prometheus` 는 404 다.
- 노출하는 엔드포인트는 `health`(상태만, 상세 없음)와 `prometheus` 둘뿐이다. `env`·`beans`·`heapdump` 등은 열지 않는다.
- Grafana(3000)·Prometheus(9090)는 `127.0.0.1` 에만 연다. Prometheus 는 인증이 없고 remote write 로 데이터를 넣을 수 있기 때문이다.
- 모니터링 컨테이너는 `monitoring` 네트워크에만 붙어 postgres·AI 서버에 닿지 않는다.

## 처음 한 번: 준비

### 1. `.env`

`.env` 는 git 에 없다. 루트에서 `.env.example` 을 복사해 채운다. 다른 PC 에서 쓰던 `.env` 가 있으면 그대로 가져오는 게 편하다.

```powershell
Copy-Item .env.example .env
```

부하 테스트에 필요한 값은 `POSTGRES_*`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod` 다.
`LLM_API_KEY` 는 픽봇·리뷰 분석에만 쓰고 부하 테스트에는 필요 없다. 로그인 시나리오까지 돌리려면 `K6_USER_EMAIL`·`K6_USER_PASSWORD` 를 채운다.

### 2. 데이터 준비

**데이터가 없으면 부하 테스트 숫자가 의미가 없다.** 카페 목록(좋아요순)은 리뷰 전체를 집계하고, 카페 상세는 리뷰를 모두 읽는다. 리뷰가 0건이면 실제보다 훨씬 가볍게 나온다.
리뷰 더미(`review_dummy.sql`)와 원본 크롤링 CSV 는 git 에 없으므로, **데이터가 있는 PC 에서 DB 를 덤프해 옮기는 방법을 권한다.**

**A. 다른 PC 의 DB 를 옮기기 (권장)**

PowerShell 의 `>` 는 바이너리를 깨뜨리므로 컨테이너 안에 파일로 만든 뒤 `docker cp` 로 꺼낸다.

```powershell
# 데이터가 있는 PC (저장소 루트)
docker compose exec postgres pg_dump -U mypickcafe -d mypickcafe -Fc -f /tmp/mypickcafe.dump
docker cp mypickcafe-postgres:/tmp/mypickcafe.dump .\mypickcafe.dump

# 옮겨 갈 PC (저장소 루트, .env 준비 후)
docker compose up -d postgres
docker cp .\mypickcafe.dump mypickcafe-postgres:/tmp/mypickcafe.dump
docker compose exec postgres pg_restore -U mypickcafe -d mypickcafe --no-owner /tmp/mypickcafe.dump
```

`-U`·`-d` 는 각 PC `.env` 의 `POSTGRES_USER`·`POSTGRES_DB` 값이다. 이미 테이블이 있는 DB 에 덮어쓸 때는 `pg_restore` 에 `--clean --if-exists` 를 붙인다.
덤프 파일은 git 에 올리지 않는다(회원 이메일·비밀번호 해시가 들어 있다).

**B. 저장소에 있는 더미만 넣기 (리뷰 없음)**

점주 225명·카페 225곳·리뷰어 100명만 있다. 화면 흐름과 모니터링 연결을 확인하는 데는 충분하지만, 성능 숫자는 낙관적으로 나온다.

```powershell
docker compose up -d postgres
# 테이블 만들기: dev 프로파일(ddl-auto=update)로 한 번 띄운다
$env:SPRING_PROFILES_ACTIVE = 'dev'; docker compose up -d --build --no-deps mypickcafe-web; Remove-Item Env:SPRING_PROFILES_ACTIVE
docker compose logs -f mypickcafe-web   # "Started MyPickCafeApplication" 이 보이면 Ctrl+C
# PowerShell 5.1 의 파이프는 한글을 ? 로 바꾸므로 파일을 컨테이너에 복사해 psql -f 로 넣는다(순서: 점주 → 카페 → 리뷰어)
foreach ($f in 'cafeowner_dummy.sql', 'cafe_dummy.sql', 'reviewer_dummy.sql') {
  docker cp "MyPickCafe_AI\Create_Dummy\$f" "mypickcafe-postgres:/tmp/$f"
  docker compose exec postgres psql -q -U mypickcafe -d mypickcafe -v ON_ERROR_STOP=1 -f "/tmp/$f"
}
# 테이블을 만든 뒤에는 prod 로 다시 띄운다(아래 "실행"의 1번)
```

더미 회원의 비밀번호는 저장소에 없다. 로그인 시나리오용 계정은 사이트의 회원가입으로 하나 만든다(비밀번호 8~20자).

## 실행

저장소 루트에서 실행한다.

### 1. 앱 띄우기

```powershell
docker compose up -d postgres
docker compose up -d --build --no-deps mypickcafe-web
```

AI 서버(`mypickcafe-ai`)는 빼고 띄운다. 부하 테스트 화면은 AI 를 부르지 않고, AI 가 처음 뜰 때 리뷰 전체를 임베딩(CPU 로 수십 분)하면서 CPU 를 차지해 숫자가 흔들린다.
전체 스택을 띄웠다면 `docker compose logs -f mypickcafe-ai` 에서 인덱싱이 끝난 뒤에 테스트한다.

### 2. 모니터링 켜기

```powershell
docker compose --profile monitoring up -d prometheus grafana
```

| 주소 | 확인할 것 |
| --- | --- |
| http://localhost:9090/targets | `mypickcafe-web` 이 **UP** |
| http://localhost:9090/alerts | 규칙 5개(AppDown, HighErrorRate, SlowResponses, DbConnectionWait, HeapNearLimit) |
| http://localhost:3000 | `admin` / `.env` 의 `GRAFANA_ADMIN_PASSWORD`(비우면 `admin`). Dashboards → MyPickCafe |

`--profile monitoring up -d` 뒤에 서비스 이름을 빼면 AI·Ollama 까지 모두 뜬다.

### 3. 테스트 — 항상 스모크 → 부하 → 스트레스 순서

```powershell
docker compose run --rm k6 run /scripts/smoke.js
docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/load.js
docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/stress.js
echo $LASTEXITCODE   # 0 = 기준 통과, 99 = 기준(threshold) 위반
```

- `-o experimental-prometheus-rw` 를 붙여야 결과가 Prometheus 로 가서 **k6 부하 테스트** 대시보드에 그려진다.
  응답 시간은 native histogram(`k6_http_req_duration_seconds`)으로 보낸다(compose 의 `K6_FEATURES`).
  원본 가이드의 `k6_http_req_duration_p95` 는 화면(name 태그)별로만 나와 전체 p95 로 합칠 수 없어서 바꿨다.
- 진행 막대가 길게 찍히면 `run --quiet` 를 붙인다.
- 사람 수만 바꿔 보려면 `docker compose run --rm -e PEAK_VUS=100 k6 run ... /scripts/load.js`.

## 부하 수치를 이렇게 잡은 이유

| 테스트 | 사용자(VU) | 화면 사이 대기 | 시간 | 기준 (어기면 종료 코드 99) |
| --- | --- | --- | --- | --- |
| smoke | 1 | 1초 | 20초 | 모든 응답이 기대한 코드 (요청 20종을 한 번씩: 비로그인 10, 로그인 10) |
| load | 50 = 비로그인 40 + 로그인 10 | 1~3초 | 4분 30초 | 실패 1% 미만, p95 500ms·p99 1.5초 미만, 로그인 p95 1초 미만 |
| stress | 20 → 100 → 200 → 300, 단계마다 1분 유지 | 없음 | 7분 | p95 1초·실패 1% (어기는 단계가 한계) |
| slow-ai | 열람 40 + 느린 AI 요청 5 → 10 → 20 → 30 | 열람 1~3초, 느린 요청 1초 | 7분 | 구간마다 열람 p95 500ms·실패 1% (아래 "느린 AI 테스트") |

- **load 50명**: 개인이 운영하는 지역 카페 탐색 서비스(EC2 t3.large 한 대)의 피크를 넉넉히 잡은 가정이다. 화면을 1~3초씩 읽으며 둘러보는 50명은 초당 약 55건의 요청이다(화면이 부르는 API 포함, 흐름에서 계산한 값). 가정이 다르면 `PEAK_VUS` 로 바꾼다.
- **화면이 부르는 API**: 브라우저는 화면 HTML 을 받은 뒤 스크립트로 API 를 더 부른다. 모든 화면에서 `cafego.js` 가 `/api/auth/me` 를 부르고(비로그인은 401 이 정상), 로그인 화면에서는 머리글이 알림 개수를 부른다. k6 도 화면마다 같이 보낸다. 이미지·CSS·JS 같은 정적 파일은 보내지 않는다(브라우저가 캐시하고, 여기서는 앱의 처리 비용을 본다).
- **기준**: 원본 가이드의 데모 API 는 일부러 10% 실패했지만 이 앱은 실패하면 안 되므로 실패율 기준을 1% 로 잡았다. 기준은 테스트 전에 정한 값이며, 결과를 보고 고치지 않는다.
- **stress 계단**: 이 앱의 대기열 두 개에 맞췄다. **20명** = prod 의 DB 커넥션 풀 크기(`DB_POOL_MAX=20`), **200명** = Tomcat 기본 스레드 수. 대기 없이 요청하므로 VU 수보다 훨씬 큰 부하다.
- **로그인 사용자 20%**: 로그인(BCrypt), 니즈 기반 추천 목록, 요청마다 JWT 필터가 하는 회원 조회, 알림 목록(30%), 마이페이지와 그 하위 화면인 내가 쓴 리뷰·즐겨찾기(50%)가 비로그인과 다른 부하를 만든다. VU 마다 처음 한 번만 로그인한다.
- **넣지 않은 것**: smoke·load·stress 에는 리뷰 작성·찜·카페 등록 같은 쓰기 요청(데이터가 쌓이고 리뷰 작성은 AI 를 부름)과 픽봇(요청마다 외부 LLM 비용이 들고 앱이 아니라 LLM 응답 시간을 재게 됨)을 넣지 않았다. 픽봇·리뷰 작성은 가짜 AI 서버를 붙인 `slow-ai.js` 에서 따로 본다.

### 결과 읽기

**k6 부하 테스트** 대시보드에서 볼 것:

| 그래프 | 읽는 법 |
| --- | --- |
| 초당 요청 수 | VU 를 늘려도 더 오르지 않는 지점이 처리량 한계 |
| 응답 시간 p95: k6 vs 앱 | 둘 다 최근 30초의 p95. 둘이 벌어지면 "처리는 빠른데 줄이 길다". 앱 지표는 스레드를 잡은 뒤부터 재고, k6 는 사용자 쪽에서 잰다 |
| 화면별 응답 시간 p95 | 어느 화면이 먼저 느려지는지(보통 `cafe-list(likes)`, `cafe-detail`) |
| 줄이 생기는 곳 | DB 커넥션 대기가 먼저 오르는지, Tomcat 스레드가 먼저 차는지 |

**MyPickCafe 웹 서버** 대시보드는 평소 상태(요청·에러·응답 시간·DB 커넥션·스레드·힙·CPU·GC·AI 호출)를 본다.

### 검증 기록 (2026-11-11, 참고용)

설정이 동작하는지 확인하려고 노트북(16스레드·16GB)에서 저장소의 더미 데이터(**카페 225곳, 리뷰 0건**)로 돌린 결과다.
리뷰가 없어 실제보다 훨씬 가볍게 나온다. 성능 판단에 쓰지 말고, 대시보드를 읽는 예로만 본다.
화면이 부르는 API(비로그인 `/api/auth/me`)와 마이페이지·알림 목록을 흐름에 넣기 전에 잰 값이라, 지금 흐름보다 요청 수가 적다.

- smoke: 화면 12종 모두 200. load: 7,438건, 실패 0, p95 21ms, 로그인 p95 71ms, 정점 초당 약 33건.
- stress: 36.8만 건, 실패 0, 전체 p95 551ms. 단계별(각 단계 유지 구간):

| VU | k6 초당 요청 | k6 p95 | 앱 p95 | DB 커넥션 사용/대기 | Tomcat 바쁜 스레드 |
| --- | --- | --- | --- | --- | --- |
| 20 | 1,201 | 31ms | 28ms | 17 / 0 | 19 |
| 100 | 963 | 268ms | 256ms | 19 / 66 | 91 |
| 200 | 925 | 604ms | 612ms | 20 / 177 | 198 |
| 300 | 934 | 785ms | 655ms | 20 / 179 | 200 |

읽는 법: 처리량은 20명에서 이미 최대이고 그 뒤로는 늘지 않는다. 100명부터 DB 커넥션 20개가 모두 쓰이고 대기가 쌓이므로, 이 조건의 첫 병목은 **DB 커넥션 풀**이다.
300명에서는 Tomcat 스레드 200개도 다 차서, k6 p95(785ms)와 앱 p95(655ms)의 차이만큼 스레드 앞에서 줄을 섰다.
JVM CPU 는 44% 정도로 여유가 있었다. 단, EC2(vCPU 2개)에서는 CPU 가 먼저 막힐 수 있으므로 실제 데이터와 아래 CPU 제한으로 다시 재야 한다.

## 느린 AI 테스트

AI 응답이 느릴 때(픽봇 추천·리뷰 작성), **AI 를 부르지 않는 열람 화면까지 느려지는지** 본다. 실제 AI 서버 대신 정해 둔 시간만큼 기다렸다가 답하는 **가짜 AI 서버**(`monitoring/ai-stub/ai_stub.py`)를 붙여서, LLM 비용 없이 잰다.

### 무엇을 보나

Spring 은 AI 응답을 Tomcat 스레드에서 기다린다(`WebClient ... .block()`). 기다리는 동안 DB 커넥션까지 잡고 있으면, 느린 요청이 커넥션 풀(prod 20개)을 다 차지해 다른 화면이 커넥션을 기다린다. 커넥션을 30초(`connection-timeout`) 안에 못 받으면 그 요청은 실패한다.

2026-11-13 이 테스트로 두 경로 모두 커넥션을 쥐고 있는 것을 확인하고 고쳤다(아래 검증 기록). 테스트는 같은 문제가 다시 생기지 않는지 보는 데 계속 쓴다.

| 요청 | 수정 전에 커넥션을 잡던 이유 | 수정 |
| --- | --- | --- |
| 리뷰 작성·수정 (`POST /reviews/new`, `/reviews/{id}/edit`) | `ReviewService.saveWithTags`·`updateWithTags` 가 `@Transactional` 안에서 태그 분석(최대 10초)을 기다렸다 | 저장 커밋 → 트랜잭션 밖에서 태그 분석 → 태그 반영 트랜잭션으로 나눔(`TransactionTemplate`) |
| 픽봇 (`POST /api/pickbot/recommend`) | `spring.jpa.open-in-view` 기본값(켜짐) 때문에, 승인된 카페 ID 를 조회할 때 받은 커넥션을 AI 응답(최대 60초)이 지나 요청이 끝날 때까지 쥐고 있었다 | AI 를 기다리는 세 경로만 open-in-view 에서 뺌(`OpenInViewConfig`). 다른 화면은 트랜잭션 밖 지연 로딩에 기대고 있어 그대로 둔다 |

단위 수준에서는 `DbConnectionDuringAiCallTest` 가 AI 클라이언트가 불린 순간 빌려 간 커넥션이 0개인지, `OpenInViewConfigTest` 가 open-in-view 가 어느 경로에 걸리는지 확인한다.

| 파일 | 역할 |
| --- | --- |
| `monitoring/ai-stub/ai_stub.py` | 가짜 AI 서버. 픽봇 추천은 5~15초, 리뷰 태그 분석은 2~4초 중 요청마다 랜덤(균등 분포)으로 기다렸다가 형식만 맞는 응답을 준다. 태그는 비워서 준다(카페 대표 태그가 바뀌지 않게). 표준 라이브러리만 쓴다 |
| `monitoring/ai-stub/docker-compose.ai-stub.yml` | 덧붙임 설정. `ai-stub` 컨테이너를 띄우고 web 의 AI 주소(`PYTHON_API_BASE_URL`·`PICKBOT_API_BASE_URL`)를 가짜 AI 서버로 바꾼다. 배포는 이 파일을 쓰지 않는다 |
| `monitoring/k6/slow-ai.js` | 열람 40명을 계속 돌리면서 느린 요청을 5 → 10 → 20 → 30건으로 늘린다. 열람 응답 시간을 구간별로 판정한다 |

### 준비

앱과 모니터링은 위 "실행"의 1·2번대로 띄운 상태에서, 저장소 루트에서 실행한다.

```powershell
# 가짜 AI 서버를 띄우고 web 을 가짜 AI 주소로 다시 만든다(실제 AI 서버는 쓰지 않으므로 꺼 둬도 된다)
docker compose -f docker-compose.yml -f monitoring/ai-stub/docker-compose.ai-stub.yml up -d --no-deps ai-stub mypickcafe-web
docker logs mypickcafe-ai-stub   # "가짜 AI 서버 :8000  픽봇 5000~15000ms, 태그 2000~4000ms"
docker compose run --rm k6 run /scripts/smoke.js   # web 을 다시 만들었으니 먼저 확인
```

- **지연 시간**: 요청마다 최소~최대 사이에서 고르게 뽑는다. `.env` 의 `AI_STUB_PICKBOT_DELAY_MIN_MS`·`_MAX_MS`(기본 5000·15000), `AI_STUB_TAG_DELAY_MIN_MS`·`_MAX_MS`(기본 2000·4000)로 바꾼다. 태그 분석은 Spring 대기 한도(10초), 픽봇은 60초보다 짧아야 실패 없이 잰다. 바꾼 뒤 위 `up` 명령을 다시 실행하면 `ai-stub` 만 다시 만들어진다.
- **CPU 제한**(위 "주의")을 쓰면 web 을 다시 만든 **뒤에** 건다. 다시 만들면 제한이 풀린다.
- **IDE 로 띄운 앱**: `python monitoring/ai-stub/ai_stub.py` 로 띄우고(`$env:PORT=18000` 처럼 로컬 AI 서버와 다른 포트), IDE 실행 설정에 `PYTHON_API_BASE_URL`·`PICKBOT_API_BASE_URL`=`http://localhost:18000` 을 넣는다. dev 프로파일은 커넥션 풀이 기본값 10개이고 SQL 로그가 느려 숫자가 prod 와 다르다.

### 실행

```powershell
# 픽봇(기본)
docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/slow-ai.js
# 리뷰 작성 — 로그인 계정(K6_USER_EMAIL) 필요, 리뷰가 실제로 쌓이므로 끝나고 아래 "정리"
docker compose run --rm -e SLOW_TARGET=review k6 run -o experimental-prometheus-rw /scripts/slow-ai.js
```

- setup 에서 픽봇을 한 번 보내 **가짜 AI 서버가 붙어 있는지 확인**하고, 아니면 시작하지 않는다. 실제 AI 서버에 이 테스트를 돌리면 LLM 호출이 수백 번 나간다.
- 단계를 바꾸려면 `-e SLOW_STEPS=10,15,20,25`, 열람 사용자 수는 `-e BROWSE_VUS=40`.
- 약 7분 걸린다. 기준을 어기는(종료 코드 99) 것이 정상일 수 있다. 처음 어기는 단계를 찾는 테스트다.

시간표(`step` 태그). 단계 사이 10초는 VU 가 늘어나는 중이라 판정에서 뺀다.

| 구간 | 시간 | 동시 느린 요청 | 판정 |
| --- | --- | --- | --- |
| `warmup` | 0:00–0:30 | 0 (열람 40명까지 늘림) | 안 함 |
| `baseline` | 0:30–1:30 | 0 | 열람 p95 500ms·실패 1% |
| `slow05` | 1:40–2:40 | 5 | 〃 |
| `slow10` | 2:50–3:50 | 10 | 〃 |
| `slow20` | 4:00–5:00 | 20 (= prod 커넥션 풀) | 〃 |
| `slow30` | 5:10–6:10 | 30 | 〃 |
| `cooldown` | 6:20–7:00 | 0 (회복하는지) | 〃 |

느린 요청 VU 하나는 요청을 하나 보내고, 응답을 받으면 1초 쉬고 다시 보낸다. 그래서 VU 수가 곧 동시에 걸려 있는 AI 호출 수다(픽봇 평균 10초 대기 + 1초 쉼이라 약 90%, 리뷰는 평균 3초 + 1초라 약 75%).

### 결과 읽기

- **터미널 요약**: `http_req_duration{scenario:browse,step:...}` 줄에서 **처음 ✗ 가 나오는 단계**가 "느린 AI 호출이 사이트 전체를 끌어내리는" 동시 호출 수다. `http_req_duration{scenario:slow,step:...}` 는 느린 요청 자체의 시간이며, AI 지연보다 길어진 만큼 Spring 안에서 줄을 선 것이다.
- **k6 부하 테스트 대시보드**의 "줄이 생기는 곳": 단계마다 **DB 커넥션 사용 수가 느린 요청 수만큼 오르는지**를 본다. 오르면 AI 를 기다리는 동안 커넥션을 잡고 있는 것이다. 20을 다 쓰면 대기가 생긴다. "화면별 응답 시간"에서는 `pickbot`·`review-write` 가 수 초라 다른 화면이 바닥에 깔리므로, 범례에서 그 둘을 끄고 본다.
- **MyPickCafe 웹 서버 대시보드**: "DB 커넥션 풀 (HikariCP)", "AI 서버 호출 수", "AI 서버 응답 시간 p95".
- **가짜 AI 서버 로그**(`docker logs mypickcafe-ai-stub`): 10초마다 대기 중인 요청 수와 최대치를 찍는다. 최대치가 VU 수보다 작으면 요청이 가짜 AI 서버에 닿기 전에 Spring 안에서 줄을 선 것이다. 커넥션 풀 크기(20)에서 멈추면 AI 를 부르기 전에 DB 커넥션을 기다린 것이다(수정 전 결과). 수정 후에는 30건이 모두 닿아, Spring 의 AI 연결 풀은 이 범위에서 막지 않는다. 로그의 최대치는 가짜 AI 서버를 띄운 뒤 전체의 최대라, 테스트마다 보려면 구간 시각의 "대기" 값을 본다.

| 보이는 것 | 뜻 |
| --- | --- |
| 커넥션 사용이 느린 요청 수만큼 오르고, `slow20` 부터 열람 ✗ | AI 를 기다리는 동안 DB 커넥션을 잡고 있다. 픽봇이면 open-in-view, 리뷰면 트랜잭션 안의 AI 호출 때문이다 |
| 커넥션 사용은 그대로이고 열람 ✓ | 커넥션은 잡지 않는다. 느린 요청이 쓰는 것은 Tomcat 스레드(200개 중 일부)뿐이다 |
| `cooldown` 도 ✗ | 느린 요청을 뺀 뒤에도 쌓인 대기가 30초 안에 풀리지 않았다 |

### 검증 기록 (2026-11-13) — 수정 전후

노트북(12 CPU, CPU 제한 없음)의 web 컨테이너(prod, 커넥션 풀 20·Tomcat 스레드 200), 실제 데이터(카페 225곳, 리뷰 27,965건), 가짜 AI 기본값(픽봇 5~15초, 태그 2~4초), 비로그인 열람 40명으로 돌렸다. 리뷰 작성 모드는 임시 테스트 계정으로 돌리고, 끝난 뒤 테스트 리뷰·알림과 계정을 지웠다.
수정 전은 `2ddbbe0`(feat/loadtest-slow-ai), 수정 후는 `fix/db-connection-during-ai-call` 의 이미지다. 커넥션·스레드 수는 각 구간 1분의 Prometheus 값(15초 수집)이다.

**픽봇 모드**

| 구간 | 열람 p95 전 → 후 | 열람 요청 수 전 → 후 | 픽봇 시간 평균 전 → 후 | DB 커넥션 사용 평균/최대 전 → 후 | 커넥션 대기 최대 전 → 후 | Tomcat 바쁜 스레드 평균 전 → 후 | 가짜 AI 에 닿은 동시 픽봇 전 → 후 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| baseline | 11ms → 11ms | 2,400 → 2,368 | – | 0.5/1 → 0/0 | 0 → 0 | 0.5 → 0 | – |
| slow05 | 11ms → 10ms | 2,402 → 2,366 | 10.0초 → 11.0초 | 4.4/6 → 0/0 | 0 → 0 | 4.4 → 3.4 | 5 → 5 |
| slow10 | 10ms → 10ms | 2,394 → 2,356 | 10.2초 → 10.2초 | 8.5/10 → 0/0 | 0 → 0 | 8.5 → 8.6 | 10 → 10 |
| slow20 | 32ms → 10ms | 2,358 → 2,390 | 10.0초 → 9.8초 | 17.6/20 → 0/0 | 11 → 0 | 20.2 → 17.8 | 20 → 20 |
| **slow30** | **5.71초 ✗ → 9.9ms ✓** | **738 → 2,374** | **14.6초 → 10.0초** | **20/20 → 0.6/2** | **35 → 0** | 48.2 → 26.6 | **20 → 30** |
| cooldown | 10ms → 10ms | 1,496 → 1,546 | – | 9.2/20 → 0/0 | 27 → 0 | 20.7 → 8.3 | – |

**리뷰 작성 모드**

| 구간 | 열람 p95 전 → 후 | 열람 요청 수 전 → 후 | 작성 시간 평균 전 → 후 | DB 커넥션 사용 평균/최대 전 → 후 | 커넥션 대기 최대 전 → 후 | Tomcat 바쁜 스레드 평균 전 → 후 |
| --- | --- | --- | --- | --- | --- | --- |
| baseline | 12ms → 10ms | 2,380 → 2,380 | – | 0.2/1 → 0.2/1 | 0 → 0 | 0.2 → 0.2 |
| slow05 | 11ms → 10ms | 2,410 → 2,370 | 3.0초 → 3.0초 | 3.1/4 → 0.1/1 | 0 → 0 | 3.1 → 3.9 |
| slow10 | 10ms → 10ms | 2,392 → 2,388 | 3.0초 → 2.9초 | 7.7/9 → 0.1/1 | 0 → 0 | 7.7 → 6.9 |
| slow20 | 10ms → 10ms | 2,404 → 2,410 | 3.0초 → 3.0초 | 14.2/17 → 0.2/1 | 0 → 0 | 14.2 → 15.1 |
| **slow30** | **634ms ✗ → 10ms ✓** | **2,072 → 2,402** | **3.6초 → 3.0초** | **19.8/20 → 0.3/1** | **31 → 0** | 31.0 → 23.2 |
| cooldown | 10ms → 10ms | 1,558 → 1,512 | – | 3.1/20 → 0.1/1 | 5 → 0 | 3.7 → 3.1 |

읽는 법:

- **수정 전**: 열람만 돌 때 커넥션은 1개 안팎인데, 느린 요청을 섞으면 **사용 커넥션이 기다리는 요청 수만큼** 올랐다. 30건에서는 20개가 다 차서 가짜 AI 에 동시에 닿은 요청이 20건에서 멈췄고(리뷰도 30건 구간 내내 20), 나머지는 AI 를 부르기도 전에 커넥션을 기다렸다. AI 와 상관없는 열람도 커넥션을 기다려 픽봇 때 p95 5.71초·처리량 1/3, 리뷰 때 634ms 까지 느려졌다. 대기가 30초를 넘지 않아 실패는 0건이었다.
- **수정 후**: 느린 요청을 아무리 섞어도 사용 커넥션은 1개 안팎이고 대기는 0이다. 30건 모두 가짜 AI 에 닿아 느린 요청 시간은 AI 지연 그대로(픽봇 평균 10초, 태그 3초)였고, 열람 p95 는 모든 구간에서 10ms 안팎이다.
- 리뷰 쪽 영향이 픽봇보다 작았던 것은 기다리는 시간이 짧아서(평균 3초 대 10초) 커넥션이 빨리 돌아왔기 때문이다. 실제 LLM 이 느려지면 픽봇처럼 커진다.
- 수정 후에도 느린 요청은 **Tomcat 스레드**를 하나씩 쓴다(30건이면 약 27개). 200개라 이 범위에서는 여유가 있지만, 동시 느린 요청이 수백 건이면 다음 한계가 된다. 그때는 AI 호출을 비동기로 바꾸거나 동시 호출 수를 제한한다.

### 정리와 되돌리기

리뷰 작성 모드로 돌렸다면 테스트 리뷰를 지운다. 내용이 `[k6-slow-ai]` 로 시작하는 리뷰와 그 알림·태그다. 가짜 AI 서버가 태그를 비워 주므로 카페 대표 태그는 바뀌지 않았다. **실제 AI 서버로 되돌리기 전에 지운다.** 남겨 두면 픽봇 인덱스에 들어갈 수 있다.

```powershell
docker compose exec postgres psql -U mypickcafe -d mypickcafe -c "DELETE FROM notification WHERE review_id IN (SELECT review_id FROM review WHERE content LIKE '[k6-slow-ai]%'); DELETE FROM review_tag WHERE review_id IN (SELECT review_id FROM review WHERE content LIKE '[k6-slow-ai]%'); DELETE FROM review WHERE content LIKE '[k6-slow-ai]%';"
```

web 을 실제 AI 서버로 되돌리고 가짜 AI 서버를 지운다.

```powershell
docker compose up -d --no-deps --force-recreate mypickcafe-web
docker rm -f mypickcafe-ai-stub
```

## 주의

- **같은 PC 에서 k6 와 앱을 같이 돌리면 k6 도 CPU 를 쓴다.** 절대값보다 추세(어느 단계에서 꺾이는지)를 본다.
- **노트북은 EC2 보다 훨씬 빠르다.** t3.large 는 vCPU 2개를 web·postgres·AI·Ollama 가 나눠 쓴다. 비슷하게 보려면 테스트 전에 CPU 를 묶는다.
  `docker update --cpus 1.5 mypickcafe-web; docker update --cpus 0.5 mypickcafe-postgres`
  원래대로: `docker compose up -d --no-deps --force-recreate mypickcafe-web postgres`
- **운영 서버에 stress 를 돌리지 않는다.** 실제 사용자에게 영향을 준다.
- dev 프로파일은 SQL 을 DEBUG 로그로 찍어 느리다. 부하 테스트는 컨테이너의 prod 프로파일로 한다.
- 원본 가이드 실습 프로젝트의 Prometheus·Grafana 가 9090·3000 을 쓰고 있으면 겹친다. 그쪽을 끄거나 `.env` 의 `PROMETHEUS_PORT`·`GRAFANA_PORT` 를 바꾼다.

## 다른 경우

### IDE 로 띄운 앱을 볼 때

IDE 의 앱도 관리 포트 8081 을 연다(`MyPickCafe_Springboot/secret.properties` 가 있어야 기동한다).
`monitoring/prometheus/prometheus.yml` 에서 `mypickcafe-web:8081` job 을 주석 처리하고 `host.docker.internal:8081` job 의 주석을 푼 뒤 `docker compose restart prometheus`.
k6 는 `.env` 에 `K6_BASE_URL=http://host.docker.internal:8080` 을 넣는다.

### EC2 에서 모니터링 켜기 (선택)

이 변경이 배포된 뒤(web 이 `monitoring` 네트워크에 붙은 뒤)에 서버 저장소 경로(`EC2_APP_DIR`)에서 실행한다. 배포된 이미지를 쓰도록 override 를 함께 넘긴다.

```bash
sudo docker compose -f docker-compose.yml -f /var/lib/mypickcafe-deploy/images.override.yml \
  --profile monitoring up -d prometheus grafana
```

- 메모리: 상시 6.5GB 에 Prometheus·Grafana 한도 각 512MB 가 더해져 t3.large(8GB)에 여유가 적다(실사용은 Prometheus 약 55MB, Grafana 약 180~220MB). `.env` 에 `PROMETHEUS_RETENTION=7d` 를 권한다.
- `.env` 의 `GRAFANA_ADMIN_PASSWORD` 를 반드시 채운다(처음 뜰 때만 적용).
- 접속은 포트를 열지 않고 SSM 포트 포워딩으로 한다(내 PC 에 Session Manager 플러그인과 `ssm:StartSession` 권한 필요).
  `aws ssm start-session --target <인스턴스ID> --document-name AWS-StartPortForwardingSession --parameters "portNumber=3000,localPortNumber=3000"`
- `deploy.sh` 는 web·ai 만 교체하므로 Prometheus·Grafana 는 배포와 상관없이 계속 돈다.

## 끄기와 정리

```powershell
docker compose --profile monitoring stop prometheus grafana   # 끄기 (데이터 유지)
docker compose --profile monitoring rm -f prometheus grafana  # 컨테이너 삭제 (데이터 유지)
docker volume rm mypickcafe_prometheus-data mypickcafe_grafana-data   # 수집한 지표·Grafana 설정까지 삭제
```

`docker compose down -v` 는 쓰지 않는다. DB(`pgdata`)·업로드 파일·Chroma 인덱스 볼륨까지 지워진다.

## 문제가 생기면

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| Targets 에서 `mypickcafe-web` 이 DOWN, `no such host` | web 컨테이너가 꺼져 있거나 이 변경 전 설정으로 떠서 `monitoring` 네트워크에 없음 | `docker compose up -d --no-deps mypickcafe-web` |
| k6 setup 에서 `승인된 카페가 0개` | DB 가 비어 있음 | 위 "데이터 준비" |
| k6 setup 에서 로그인 실패 | `.env` 의 `K6_USER_*` 가 틀림 | 그대로 VU 마다 로그인하면 로그인 제한(5회 실패 → 5분 차단)에 걸리므로 테스트를 시작하지 않는다. 계정을 확인한다 |
| 대시보드를 고쳤는데 그대로 | Grafana 는 시작할 때만 읽음 | `docker compose restart grafana` |
| k6 대시보드가 비어 있음 | `-o experimental-prometheus-rw` 를 빼고 실행함 | 붙여서 다시 실행 |
| 테스트에서 `/actuator/prometheus` 가 404 | Spring Boot 테스트는 기본으로 지표 내보내기를 끔 | 테스트 클래스에 `@AutoConfigureObservability` |
| `docker` 가 `cannot find the file specified` | Docker Desktop 이 꺼져 있음 | 켜고 1분쯤 기다린다 |
