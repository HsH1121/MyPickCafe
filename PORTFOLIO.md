# MyPickCafe — 리팩토링 어필 포인트 (Spring Boot 백엔드 중심)

> 학교 팀 프로젝트 **GoCafe**를 개인 프로젝트 **MyPickCafe**로 전면 리팩토링하며
> 수행한 백엔드 개선 사항 정리. 면접 답변용 근거 자료.

---

## 0. 한 줄 요약

> "팀 프로젝트로 만든 카페 정보 서비스를 개인 프로젝트로 가져와,
> **인증 없이 전 회원의 비밀번호 해시가 노출되던 취약점을 발견해 차단**하고,
> **인가 체계를 컨트롤러에서 Security 필터 체인으로 끌어올렸으며**,
> **DB를 Oracle에서 PostgreSQL로 마이그레이션**하고
> **외부 AI 서버 연동에 장애 격리를 설계**했습니다."

| 구분 | GoCafe (팀) | MyPickCafe (개인) |
|---|---|---|
| **자격증명 노출** | **`GET /api/members`로 전 회원 해시 덤프 가능** | **DTO + 직렬화 차단 + 인가, 회귀 테스트로 고정** |
| 인가 | `/api/**` 전면 permitAll | URL 패턴별 역할 기반(RBAC) + 리소스 소유권 검증 |
| API 응답 | JPA 엔티티 그대로 반환 | Request/Response DTO 분리 |
| DB | Oracle(ojdbc11) + H2 병행 | PostgreSQL 16 (Docker) 단일화 |
| 시크릿 | 소스에 하드코딩 | `secret.properties` 외부화 |
| 환경 설정 | 단일 파일 (개발용 설정 그대로) | dev / prod 프로파일 분리 |
| 외부 연동 | 없음 | Client 계층 캡슐화 + 타임아웃 + 장애 격리 |
| 태그 산출 | 수기 등록 / 문자열 코드 | 리뷰 기반 자동 재집계 + 타입 안전 enum |
| 테스트 | `contextLoads()` 1개 (DB 없으면 실패) | 21개 / 실패 0 (H2로 Docker 없이 실행) |
| API 문서 | 없음 | OpenAPI 3 + Swagger UI (dev 한정) |

---

## 1. Spring Security — 인가 체계 재설계 ⭐ 핵심

### 문제

GoCafe의 `SecurityConfig`는 사실상 API 전체가 열려 있었습니다.

```java
// GoCafe — 인가가 없는 것과 마찬가지
.requestMatchers(HttpMethod.GET,  "/api/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/**").permitAll()
.requestMatchers("/h2-console/**").permitAll()
```

POST까지 전부 열려 있어 **인가 판단이 각 컨트롤러 내부 코드로 흩어져** 있었고,
누락되면 그대로 보안 구멍이 되는 구조였습니다.

### 해결

권한 판단을 **선언적으로 Security 필터 체인에 집중**시켰습니다.

```java
// MyPickCafe — 역할별 URL 패턴 분리
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/cafes/new", "/cafes/create").hasAnyRole("CAFEOWNER", "ADMIN")
.requestMatchers("/api/cafes/*/photos", "/api/cafes/photos/**").hasAnyRole("CAFEOWNER", "ADMIN")
.requestMatchers("/api/menus/**").hasAnyRole("CAFEOWNER", "ADMIN")
.requestMatchers("/reviews/**", "/favorites/**", "/member/**").authenticated()
.anyRequest().authenticated()
```

`MEMBER / CAFEOWNER / ADMIN` 3단계 역할이 실제로 강제되도록 만들었습니다.

### 부가: 요청 유형별 인증 실패 응답 분기

같은 서버가 REST API와 Mustache 렌더링 페이지를 **동시에** 서빙하는 구조라,
인증 실패 시 응답이 달라야 했습니다. (API는 401 JSON, 브라우저는 로그인 페이지)

```java
private boolean isApiRequest(HttpServletRequest req) {
    if (req.getRequestURI().startsWith("/api/")) return true;
    String accept = req.getHeader("Accept");
    return accept != null && accept.contains("application/json");
}
```

```java
.authenticationEntryPoint((req, res, ex) -> {
    if (isApiRequest(req)) res.sendError(SC_UNAUTHORIZED, "Unauthorized");
    else                   res.sendRedirect("/login");
})
```

> **예상 질문**: "왜 컨트롤러가 아니라 SecurityConfig에서 처리했나요?"
> **답변**: 인가 규칙이 컨트롤러에 흩어지면 (1) 신규 엔드포인트 추가 시 누락 위험,
> (2) 전체 정책 파악 불가, (3) 테스트 어려움. 한곳에 모으면 정책이 문서가 됩니다.

### ⭐ 리팩토링 중 발견해서 막은 실제 취약점 — 비밀번호 해시 노출

**면접에서 가장 강력하게 쓸 수 있는 사례.** 위 인가 정리 이후에도
"엔티티를 그대로 반환하는" 컨트롤러가 남아 있었고, 두 문제가 겹쳐
**인증 없이 전 회원의 이메일과 BCrypt 해시를 덤프할 수 있는 상태**였습니다.

세 가지 조건이 동시에 성립했습니다.

```java
// (1) 엔티티를 그대로 응답으로 반환
@GetMapping
public List<Member> getAll() { return service.findAll(); }

// (2) password 필드에 직렬화 차단이 없음
@Column(name = "password", nullable = false, length = 100)
private String password;              // ← @JsonIgnore 없음

// (3) 해당 경로가 인증 없이 열려 있음
.requestMatchers(HttpMethod.GET, "/api/**").permitAll()
```

→ `GET /api/members` 한 번이면 전 회원 자격증명이 그대로 응답됩니다.

같은 문제가 **연관관계를 타고** 두 경로에서 더 재현됐습니다.

| 경로 | 유출 경로 | 인증 필요 여부 |
|---|---|---|
| `GET /api/members` | `Member.password` 직접 | 불필요 (공개) |
| `GET /api/cafes` | `Cafe.owner` → `Member.password` | 불필요 (공개) |
| `GET /api/menus/by-cafe/{id}` | `Menu.cafe` → `owner` → `password` | 불필요 (공개) |

**조치 (다중 방어)**

1. **DTO 분리** — `MemberResponse`, `CafeResponse`, `MenuResponse`로 응답 필드를 화이트리스트 방식으로 명시
2. **엔티티 레벨 차단** — 혹시 엔티티가 직렬화되더라도 새지 않도록
   ```java
   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)  // 역직렬화만 허용
   private String password;
   ```
3. **인가 강화** — `/api/members/**`를 ADMIN 전용으로, 단 **`GET /api/** permitAll`보다 먼저 선언**
   (Spring Security는 최초 매칭 규칙이 이기므로 순서가 곧 보안입니다)
4. **회귀 테스트로 고정** — 아래 §11 참고

> **예상 질문**: "엔티티를 API에 그대로 쓰면 왜 안 되나요?"
> **답변**: 교과서적으로는 "계층 결합" 때문이지만, 제 프로젝트에서는
> 실제로 **비밀번호 해시가 인증 없이 노출되는 취약점**으로 이어졌습니다.
> 엔티티는 DB 스키마를 표현하는 모델이지 API 계약이 아니고,
> 연관관계를 타고 의도치 않은 데이터까지 따라 나갑니다.

---

## 2. DB 마이그레이션 — Oracle + H2 → PostgreSQL (Docker)

### 배경

GoCafe는 Oracle(`ojdbc11`)을 쓰면서 H2도 함께 물려 있어, 로컬 환경마다 동작이 달랐습니다.
팀원마다 Oracle 설치 상태가 달라 "제 PC에선 되는데요" 문제가 반복됐습니다.

### 수행

- `ojdbc11` 제거 → `org.postgresql:postgresql`
- Hibernate dialect를 `PostgreSQLDialect`로 전환
- `docker-compose.yml`로 PostgreSQL 16 컨테이너화 → **환경 재현성 확보**
- 접속 정보를 환경변수로 주입 (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`)

### 실제로 터진 이슈 (면접에서 구체성 어필)

단순히 드라이버만 바꾼 게 아니라, 벤더 차이로 인한 후속 버그를 잡았습니다.
(커밋 `b357764` — "PostgreSQL 마이그레이션 후속 정리: 테이블/컬럼명 및 boolean 비교 수정")

| 이슈 | Oracle | PostgreSQL | 조치 |
|---|---|---|---|
| boolean | `NUMBER(1)` + `= 1` 비교 | 네이티브 `BOOLEAN` | 네이티브 쿼리 비교식 수정 |
| 식별자 | 큰따옴표 대문자 관례 | 기본 소문자 폴딩 | 테이블/컬럼명 정리 |
| 시퀀스 | `SET DEFINE OFF` 등 SQL*Plus 문법 | 불필요 | 더미 SQL 문법 재작성 |

> **예상 질문**: "마이그레이션에서 가장 어려웠던 점은?"
> **답변**: JPA로 추상화된 부분은 dialect 교체로 해결됐지만,
> **네이티브 쿼리는 자동 변환이 안 돼서** 직접 찾아 고쳐야 했습니다.
> 이 경험으로 네이티브 쿼리는 벤더 종속성을 만든다는 걸 체감했습니다.

---

## 3. 외부 AI 서버 연동 — 계층 분리 + 장애 격리 ⭐ 핵심

### 설계 원칙: 도메인 로직이 외부 API를 몰라야 한다

FastAPI(AI 서버) 연동이 추가됐지만, Service/Controller가 HTTP 세부사항을
직접 다루지 않도록 **전용 Client 계층**으로 감쌌습니다.

```
ReviewService  →  PythonTagClient  →  FastAPI /review/analyze
               →  ChatbotClient    →  FastAPI /chatbot/recommend
```

- 전용 DTO(`PythonTagRequest/Response`, `ChatbotRequest/Result`)로 경계 정의
- 엔드포인트 URL은 `application.properties`로 외부화 (`python.api.base-url`)

### 장애 격리 (Graceful Degradation) — 가장 어필하기 좋은 부분

AI 서버는 **부가 기능**이므로, 죽어도 핵심 기능(리뷰 작성)은 동작해야 합니다.

```java
public Optional<PythonTagResponse> analyze(PythonTagRequest req) {
    try {
        ResponseEntity<PythonTagResponse> res = restTemplate.postForEntity(...);
        if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
            return Optional.of(res.getBody());
        }
    } catch (Exception e) {
        log.warn("Python tag API 호출 실패 (무시됨): {}", e.getMessage());
    }
    return Optional.empty();   // ← 실패해도 리뷰 저장은 이미 완료된 상태
}
```

호출부에서도 이 계약이 그대로 드러납니다.

```java
Review saved = reviewRepository.save(review);   // 1) 리뷰 저장 먼저 (필수)
pythonTagClient.analyze(pyReq).ifPresent(res -> {  // 2) 태그는 부가 (실패 시 skip)
    saveEnumTags(saved, res);
    syncCafeTopTags(saved.getCafe().getId());
});
```

**저장 순서 자체가 설계 의도**입니다. AI 호출 결과를 기다렸다가 저장하면
AI 서버 장애가 곧 리뷰 기능 장애가 되므로, 저장을 먼저 확정했습니다.

### 비동기 인덱싱

벡터 DB(ChromaDB) 동기화는 사용자 응답을 지연시킬 이유가 없어 비동기 처리했습니다.

```java
public void indexOneAsync(ChatbotIndexRequest req) {
    CompletableFuture.runAsync(() -> {
        try { restTemplate.postForEntity(baseUrl + "/chatbot/index-one", req, Void.class); }
        catch (Exception e) { log.warn("Chatbot index-one 호출 실패 (무시됨): {}", e.getMessage()); }
    });
}
```

---

## 4. 도메인 모델 개선 — 타입 안전성 + 실제 버그 수정

### 4-1. 문자열 코드 → 타입 안전 enum 체계

GoCafe는 태그가 `String code` 하나로만 관리돼 오타가 런타임까지 갔습니다.
MyPickCafe는 **공통 인터페이스 + 4개 카테고리 enum**으로 재설계했습니다.

```java
public interface TagEnum {
    String name();
    String getLabel();          // "와이파이"  (화면 표시용 한글)
    String getCategory();       // "FACILITY"  (분류 코드)
    String getCategoryLabel();  // "시설"
}
```

```java
public enum FacilityTag implements TagEnum {
    PLUG("콘센트"), TERRACE("테라스"), PET("반려동물"), PARKING("주차"), WIFI("와이파이");
    // ...
    @Override public String getCategory() { return "FACILITY"; }
}
```

`FacilityTag / MenuTag / PurposeTag / MoodTag` 4종이 같은 인터페이스를 구현해,
**화면 표시 로직을 카테고리별로 분기하지 않고 일괄 처리**할 수 있게 됐습니다.

```java
// 카테고리 상관없이 동일하게 처리
private void saveTagDesc(TagEnum tag) {
    tagDictionaryRepository.save(TagDictionary.builder()
        .tagCategory(tag.getCategory())
        .tagEnum(tag.name())
        .tagLabel(tag.getLabel())
        .build());
}
```

### 4-2. 유니크 제약 버그 수정 (실제 데이터 정합성 문제)

```java
// GoCafe — 카테고리가 다른데도 같은 코드면 저장 불가 (버그)
@UniqueConstraint(columnNames = {"review_id", "code"})

// MyPickCafe — 카테고리까지 포함해야 올바른 식별자
@UniqueConstraint(columnNames = {"review_id", "category_code", "code"})
```

> 예: `MENU=CAKE`와 `MOOD=CAKE`처럼 카테고리가 다르면 공존 가능해야 하는데,
> 기존 제약은 이를 막고 있었습니다. **복합키 설계 오류를 발견해 수정한 사례.**

### 4-3. 순환 참조 차단

양방향 연관관계를 JSON 직렬화할 때 무한 재귀가 발생하는 문제를 `@JsonIgnore`로 차단.

```java
@JsonIgnore
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "review_id", nullable = false)
private Review review;
```

---

## 5. 리뷰 태그 → 카페 대표 태그 자동 재집계

리뷰가 쌓일수록 카페의 성격이 자동으로 갱신되는 **집계 로직**을 직접 설계했습니다.

### 집계 쿼리

```sql
SELECT t.category_code, t.code, COUNT(*) AS cnt
  FROM review_tag t
  JOIN review r ON r.review_id = t.review_id
 WHERE r.cafe_id = :cafeId
   AND r.sentiment = 'GOOD'                                   -- 긍정 리뷰만 반영
   AND t.category_code IN ('FACILITY','MENU','PURPOSE','MOOD')
 GROUP BY t.category_code, t.code
 ORDER BY t.category_code, cnt DESC
```

### 상대 임계값(85%) 채택

상위 N개를 자르는 대신 **1위 대비 85% 이상**인 태그를 모두 채택했습니다.

```java
long topCount = ((Number) tagCounts.get(0)[2]).longValue();
for (Object[] tagCount : tagCounts) {
    long cnt = ((Number) tagCount[2]).longValue();
    if (cnt < topCount * 0.85) break;   // 1위 대비 85% 미만이면 중단
    // ... CafeTag 저장
}
```

> **왜 Top-N이 아니라 상대 임계값인가?**
> - Top-3 고정: 10표/9표/9표/9표일 때 동률인 4번째가 부당하게 탈락
> - 절대 임계값(예: 5표 이상): 리뷰 적은 신규 카페는 태그가 아예 안 붙음
> - **상대 임계값**: 리뷰 수와 무관하게 "압도적인 태그만" 선별 → 두 문제 동시 해결

### 트리거 시점

리뷰 **작성 / 수정 / 삭제** 세 시점 모두에서 재집계가 돌도록 연결해,
데이터가 항상 최신 상태를 유지합니다.

```java
@Transactional
public void delete(Long id) {
    Review review = reviewRepository.findById(id).orElseThrow(...);
    Long cafeId = review.getCafe().getId();
    chatbotClient.deleteOneAsync(id);
    reviewRepository.deleteById(id);
    syncCafeTopTags(cafeId);        // ← 삭제 후에도 재집계
}
```

> **주의해서 답변할 것**: 삭제 시 `cafeId`를 **미리 꺼내둔 이유**는
> 삭제 후에는 연관 엔티티 접근이 불가능하기 때문. 이런 디테일을 짚으면 좋습니다.

---

## 6. 설정 외부화 (Externalized Configuration)

GoCafe는 DB 비밀번호와 JWT 시크릿이 소스에 그대로 있어 Git에 올라갔습니다.

```properties
# MyPickCafe — 시크릿을 저장소 밖으로 분리
spring.config.import=file:./secret.properties

spring.datasource.password=${DB_PASSWORD}
app.jwt.secret=${JWT_SECRET}
kakao.js-key=${KAKAO_JS_KEY:}     # 기본값 문법으로 미설정 시에도 기동 가능
```

`secret.properties`는 `.gitignore` 처리하고, `docker-compose.yml`도 하드코딩 대신
환경변수를 받도록 구성했습니다.

---

## 7. 성능 — N+1 대응

카페 목록에서 각 카페의 태그/사진을 조회할 때 N+1이 발생했습니다.

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=100
```

지연 로딩된 연관 엔티티를 **`IN` 절로 묶어 한 번에** 가져오도록 설정.
`N+1` → `1 + (N/100)` 쿼리로 감소.

> **예상 질문**: "fetch join을 쓰지 않은 이유는?"
> **답변**: fetch join은 컬렉션이 둘 이상이면 `MultipleBagFetchException`이 발생하고
> 페이징과 함께 쓰면 메모리 페이징 문제가 생깁니다. batch size는 전역으로 적용돼
> 페이징과 안전하게 공존하므로 목록 화면에는 이쪽이 적합했습니다.

---

## 8. 개발 프로세스 개선

코드뿐 아니라 **이력 관리 습관**도 정비했습니다.

| | GoCafe | MyPickCafe |
|---|---|---|
| 커밋 메시지 | `commit001`, `commit002: oracle error`, `Mypage design Changi`(오타 반복) | `PostgreSQL 마이그레이션 후속 정리: 테이블/컬럼명 및 boolean 비교 수정` |
| 브랜치 | 개인명 브랜치(`HsH`, `Jin`, `YG`, `Changi`) 난립 | 기능 단위(`db-migration-postgres-docker`, `cafe-recommendation-chatbot`) |
| 문서화 | `readme.md` 1줄 | `SETUP.md`, `description.txt`, 실험 기록 문서 |

> **What(무엇을)이 아니라 Why(왜)를 남기는 커밋**으로 바꿨고,
> 브랜치를 사람이 아닌 **기능 단위**로 끊어 머지 이력이 읽히게 만들었습니다.

---

## 9. 정량적 근거를 남기는 습관

LLM 파라미터를 감으로 정하지 않고, **F1 스코어로 비교 실험 후 문서화**했습니다.
(`model_settings_comparison.txt`)

| 설정 | temperature | top_p | 완전일치 | 전체 F1 |
|---|---|---|---|---|
| 배치 생성용 | 0.0 | 기본값 | 14/15 (93%) | 0.98 |
| 실시간 API용 | 0.1 | 0.9 | 12/15 (80%) | 0.97 |

- `temperature=0.0` → 재현성 확보, 배치 대량 생성에 유리
- `top_p=0.9` → 허용 목록 외 오탐(MOOD FP) 0건

> **용도에 따라 다른 설정을 쓰는 게 맞다**는 결론을 데이터로 증명한 사례.
> "AI를 갖다 썼다"가 아니라 "검증하고 튜닝했다"로 이야기할 수 있는 근거입니다.

---

## 면접 예상 질문 대비 요약

| 질문 | 핵심 답변 | 근거 위치 |
|---|---|---|
| **가장 크게 개선한 부분은?** | **인증 없이 비밀번호 해시가 노출되던 취약점 발견·차단** | **§1** |
| 가장 크게 개선한 부분은? (구조) | 인가를 컨트롤러에서 Security 필터 체인으로 이동 | §1 |
| 엔티티를 API에 쓰면 왜 안 되나? | 실제로 연관관계를 타고 자격증명이 유출됨 | §1 |
| 리팩토링 중 발견한 버그는? | ReviewTag 복합 유니크 제약 누락 / `/api/menus` 중복 매핑 | §4-2, §10-5 |
| 외부 API 장애 대응은? | 저장 우선 + Optional 반환 + 타임아웃 명시 | §3, §10-2 |
| 성능 이슈 경험은? | 목록 조회 N+1 → batch fetch size | §7 |
| 설계 판단을 내린 사례는? | Top-N 대신 상대 임계값(85%) 채택 | §5 |
| DB 마이그레이션 난점은? | 네이티브 쿼리는 자동 변환이 안 됨 | §2 |
| 운영 배포를 고려한 부분은? | 프로파일 분리, prod `ddl-auto=validate` | §10-1 |
| 테스트는 어떤 기준으로? | 회귀하면 치명적인 것(인가·자격증명)부터 고정 | §11 |
| 더 개선한다면? | CI/CD, OSIV 정리, 재집계 로직 테스트 | §12 |

---

## 10. 운영 관점 개선

### 10-1. 환경별 설정 프로파일 분리

기존에는 단일 `application.properties`에 개발용 설정이 그대로 들어 있었습니다.
그대로 배포하면 **스택트레이스와 SQL이 그대로 노출**되는 구성입니다.

| 설정 | 기존 (단일 파일) | dev | prod |
|---|---|---|---|
| `include-stacktrace` | `ALWAYS` | `ALWAYS` | `never` |
| `ddl-auto` | `update` | `update` | `validate` |
| Security 로깅 | `DEBUG` | `DEBUG` | `WARN` |
| 쿠키 `Secure` | `false` | `false` | `true` |
| CORS 허용 오리진 | 코드에 `"*"` 하드코딩 | `*` | 환경변수 화이트리스트 |
| Swagger UI | — | 활성 | 비활성 |

특히 **`ddl-auto`를 prod에서 `validate`로 바꾼 것**이 핵심입니다.
`update`는 운영 스키마를 자동 변경해 데이터 손상 위험이 있고,
`validate`는 엔티티와 스키마가 어긋나면 기동 자체를 중단시킵니다.

CORS도 코드 상수에서 설정으로 뺐습니다.

```java
// Before: config.setAllowedOrigins(List.of("*"));  // dev only 주석만 달린 채 운영에도 적용
// After
config.setAllowedOriginPatterns(
        Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
```

### 10-2. 외부 호출 타임아웃 — RestTemplate → WebClient

기존에는 옵션 없는 `new RestTemplate()`을 공유했습니다.
**타임아웃이 무제한**이라 AI 서버가 응답을 붙잡고 있으면 리뷰 작성 요청
스레드가 그대로 묶여, 부가 기능 장애가 핵심 기능 장애로 번지는 구조였습니다.

```java
HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)  // 2초
        .responseTimeout(Duration.ofMillis(readTimeoutMs))               // 10초
```

비동기 색인도 개선했습니다.

```java
// Before: CompletableFuture.runAsync(...)  — 블로킹 호출을 별도 스레드가 점유
// After : WebClient는 논블로킹이라 subscribe()만으로 스레드 점유 없이 발사
        .bodyToMono(Void.class)
        .subscribe(ignored -> {}, e -> log.warn("색인 실패 (무시됨): {}", e.getMessage()));
```

> **예상 질문**: "MVC 프로젝트에 왜 WebClient를 썼나요?"
> **답변**: RestTemplate은 유지보수 모드이고, 무엇보다 타임아웃·커넥션 풀을
> 선언적으로 설정하기가 WebClient 쪽이 명확합니다. 서블릿 스택이라
> 동기 호출은 `block()`으로 받지만, fire-and-forget 색인은 논블로킹의
> 이점을 그대로 활용했습니다.

### 10-3. API 문서화 (springdoc-openapi)

`/swagger-ui.html`로 API를 탐색할 수 있게 하고, JWT Bearer 스킴을 등록해
**문서에서 바로 인증 후 호출 테스트**가 가능하도록 했습니다.
prod 프로파일에서는 `springdoc.api-docs.enabled=false`로 꺼서 API 표면을
공개하지 않습니다.

### 10-4. 리소스 단위 소유권 검증 (IDOR 방지)

URL 기반 인가는 "CAFEOWNER 역할인가"까지만 판단할 수 있습니다.
"**이** 카페의 점주인가"는 리소스를 조회해야 알 수 있어, 이 검증이 없으면
**점주 A가 점주 B의 메뉴를 수정**할 수 있었습니다.

여러 컨트롤러에 중복돼 있던 검증 로직을 `CafeOwnershipGuard`로 모으고
메뉴 API에도 적용했습니다.

```java
@Component
public class CafeOwnershipGuard {
    public void ensureOwner(Authentication auth, Cafe cafe) { ... }  // 점주 본인 or ADMIN
}
```

### 10-5. 중복·미사용 엔드포인트 정리

- `MenuController`와 `MenuApiController`가 **둘 다 `/api/menus`에 매핑**돼 있던 중복 제거
- 프론트엔드에서 전혀 호출되지 않으면서 엔티티를 노출하던 CRUD 스캐폴딩 3종
  (`/api/cafe-tags`, `/api/categories`, `/api/needs`) 삭제 → **공격 표면 축소**

---

## 11. 테스트 — 개선 사항을 회귀 테스트로 고정

기존에는 `contextLoads()` 하나뿐이었고, 그마저도 **로컬 PostgreSQL이 없으면 실패**했습니다.
H2 인메모리 기반 `test` 프로파일을 추가해 Docker 없이도 전체 스위트가 돌아가게 만들었습니다.

**현재: 21개 테스트 / 실패 0**

| 테스트 | 검증 대상 | 개수 |
|---|---|---|
| `ApiAuthorizationTest` | 역할별 인가 규칙 (비로그인/MEMBER/ADMIN) | 7 |
| `MemberResponseLeakTest` | **비밀번호 해시 미노출** (회원·카페 두 경로) | 2 |
| `MemberServiceTest` | 비밀번호 인코딩, 중복 검증, null 필드 무시 | 5 |
| `CafeServiceTest` | 승인 상태 강제, 소유자 지정, 중복 거부 | 3 |
| `AiClientDegradationTest` | **AI 서버 장애 시 graceful degradation** | 3 |
| `MyPickCafeApplicationTests` | 컨텍스트 로드 | 1 |

가장 의미 있는 두 가지:

```java
// (1) 취약점 회귀 방지 — 응답 어디에도 해시가 없어야 한다
assertThat(body)
        .doesNotContain(BCRYPT_HASH)
        .doesNotContain("$2a$")
        .doesNotContain("leak-test@example.com");
assertThat(body).contains("leak-test-owner");   // 필요한 데이터는 남아있음(테스트 유효성 확인)

// (2) 장애 격리 — 죽은 포트로 실제 호출해 실패 경로를 그대로 태운다
PythonTagClient client = new PythonTagClient(WebClient.create("http://localhost:19999"));
assertThat(client.analyze(request)).isEmpty();   // 예외 전파 없이 흡수
```

> **예상 질문**: "테스트는 어떤 기준으로 작성했나요?"
> **답변**: 커버리지 숫자를 올리기보다 **회귀하면 치명적인 것**부터 고정했습니다.
> 인가 규칙과 비밀번호 노출은 실수하면 바로 사고로 이어지고, 코드만 봐서는
> 안전한지 확신할 수 없는 영역이라 테스트로 못 박는 게 가장 효과적이라고 판단했습니다.

---

## 12. 남은 개선 과제 (솔직하게 말할 거리)

면접에서 "더 개선한다면?"이라는 질문에 쓸 수 있는 항목입니다.

- **CI/CD 부재** — GitHub Actions로 빌드·테스트 자동화 필요
- **`spring.jpa.open-in-view`** 기본값(true) 유지 중 — API 경로는 서비스 계층에서
  DTO 매핑을 끝내도록 정리했지만, 뷰 렌더링 경로는 아직 OSIV에 의존
- **N+1 추가 최적화** — `CafeResponse` 매핑 시 `owner` 조회가 batch fetch에
  의존. fetch join 전용 쿼리로 더 줄일 여지 있음
- **테스트 확대** — 리뷰 태그 재집계(85% 임계값) 로직에 대한 단위 테스트 미작성
- **`CategoryService`** — 스캐폴딩 컨트롤러 삭제로 현재 미사용 상태 (정리 대상)
