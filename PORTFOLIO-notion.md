# GoCafe → MyPickCafe 리팩토링 어필 포인트

학교 팀 프로젝트 **GoCafe**를 인수해 개인 프로젝트 **MyPickCafe**로 전면 재작업한 내용입니다. 이 문서의 모든 Before/After는 두 저장소의 실제 코드를 대조해 확인한 것입니다.

---

## 📌 한 줄 요약

> 팀 프로젝트를 인수해 보니 **JWT 서명 키가 저장소에 커밋돼 있어 인증 체계가 무력화된 상태**였고, **인증 없이 전 회원의 비밀번호 해시를 덤프**할 수 있었습니다. 이런 결함이 살아남은 근본 원인이 **회귀를 잡을 검증 수단이 없었던 것**이라 판단해, 고치는 것과 테스트로 고정하는 것을 한 세트로 진행했습니다.

### 구조적 결함 (설계·프로세스 차원)

| 구분 | GoCafe (팀) | MyPickCafe (개인) |
| --- | --- | --- |
| **토큰 보안** | **JWT 서명 키가 저장소에 커밋 → 토큰 위조 가능** | 시크릿 외부화 + 환경변수 주입 |
| **인증 우회** | **평문 비밀번호 폴백 + 인코딩 없는 회원 생성 API** | 해시 검증 단일화, 인코딩을 서비스 계층으로 |
| **인가** | **읽기·쓰기 전부 permitAll, 컨트롤러에 분산** | URL 패턴별 RBAC + 리소스 소유권 검증 |
| **API 경계** | **JPA 엔티티 직접 노출 → 자격증명 유출** | Request/Response DTO + 직렬화 차단 |
| **검증 수단** | **테스트 1개, 그마저 로컬 Oracle 필수** | 21개 / 실패 0, Docker 없이 실행 |
| **환경 분리** | 단일 파일 (개발용 설정이 운영에 그대로) | dev / prod 프로파일 분리 |

### 도메인·데이터 모델

| 구분 | GoCafe (팀) | MyPickCafe (개인) |
| --- | --- | --- |
| 태그 저장 | `liked_tag_csv` 문자열 컬럼, 태그 목록은 `@Transient` | 정규화 테이블 + 4종 enum |
| 타입 안전성 | 리플렉션으로 getter 이름 추측 | 타입 안전 enum |
| 태그 생성 | 수기 등록 | 리뷰 기반 AI 추출 + 자동 재집계 |
| 권한 승격 | 카페 *등록만* 해도 점주 (승인 절차 무력화) | 관리자 승인 시점에만 승격 |
| DB | Oracle + H2 병행 | PostgreSQL 16 (Docker) |

---

## 1️⃣ 인수 시점의 구조적 결함

버그 몇 개가 아니라 **설계 차원에서 빠져 있던 것들**입니다. 네 가지가 서로 맞물려 있었습니다.

### 1-1. 인증 체계가 사실상 무력화돼 있었다

JWT 구현 자체는 멀쩡합니다. HS256 서명 검증, 만료 처리, `tokenVersion` 기반 서버측 무효화까지 갖춰져 있습니다. 문제는 **서명 키가 저장소에 커밋돼 있었다**는 것입니다.

```properties
# GoCafe application.properties — Git에 그대로
app.jwt.secret=b+QWlwcbxeaY...(생략)...U3A=
spring.datasource.password=jsp2025
```

| 확인 항목 | 결과 |
| --- | --- |
| 최초 커밋 | 2025-08-29 (`API & Login`) |
| 시크릿이 포함된 커밋 | 3건 |
| `.gitignore`의 시크릿 항목 | **없음** |

서명 키가 공개되면 **누구나 임의의 사용자로 토큰을 위조**할 수 있습니다. 검증 로직이 아무리 정확해도 의미가 없습니다. 저장소를 본 사람은 ADMIN 권한 토큰을 직접 만들어 넣을 수 있습니다.

인증 우회 경로도 두 개 더 있었습니다.

```java
// (a) 로그인: 해시가 안 맞으면 평문 비교로 폴백
if (!matches) matches = pw.equals(member.getPassword()); // dev only

// (b) 회원 생성 API: 비밀번호를 인코딩 없이 그대로 저장
public ResponseEntity<Member> create(@RequestBody @Valid Member body, ...) {
    Member saved = service.create(body);   // 평문이 그대로 들어감
```

(b)로 평문 계정을 만들면 (a)를 통해 그대로 로그인됩니다. **두 결함이 결합해야 완성되는 우회 경로**라, 각각만 보면 놓치기 쉽습니다.

### 1-2. 인가 계층이 없었다

```java
.requestMatchers(HttpMethod.GET,  "/api/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/**").permitAll()
```

읽기뿐 아니라 **쓰기까지 전부 열려** 있었습니다. 인가 판단이 각 컨트롤러 내부로 흩어져, 신규 엔드포인트를 추가하며 체크를 빠뜨리면 그대로 구멍이 됩니다. "막는 게 기본이고 여는 게 예외"가 아니라 그 반대인 구조였습니다.

리소스 단위 소유권 검증도 없어, 점주 A가 점주 B의 메뉴를 수정할 수 있었습니다.

### 1-3. API 계층 분리 원칙이 없었다 → 자격증명 유출

컨트롤러가 JPA 엔티티를 그대로 주고받았습니다. `Member.password`에 직렬화 차단이 없어, 1-2와 겹치면 **인증 없이 전 회원의 이메일과 BCrypt 해시를 덤프**할 수 있었습니다.

| 경로 | 유출 경로 | 인증 |
| --- | --- | --- |
| `GET /api/members` | `Member.password` 직접 | 불필요 |
| `GET /api/cafes` | `Cafe.owner` → `password` | 불필요 |
| `GET /api/menus/by-cafe/{id}` | `Menu.cafe` → `owner` → `password` | 불필요 |

주목할 점은 **아무도 이 세 곳을 "노출하려고" 만들지 않았다**는 것입니다. 엔티티를 반환하니 연관관계를 타고 따라 나갔을 뿐입니다. 개별 실수가 아니라 경계 설계가 없어서 생긴 구조적 결과입니다.

### 1-4. 회귀를 잡을 수단이 없었다 ← 위 셋의 근본 원인

```java
@SpringBootTest
class GoCafeApplicationTests {
    @Test
    void contextLoads() { }
}
```

테스트는 이것 하나였고, `@SpringBootTest`라 **로컬에 Oracle이 떠 있어야만** 돌아갑니다. 사실상 아무도 실행할 수 없는 테스트입니다.

이게 핵심입니다. **1-1~1-3이 존재할 수 있었던 이유가 1-4**입니다. 인가 규칙이 열려 있는지, 응답에 해시가 섞여 나가는지는 코드를 눈으로 봐서는 확신할 수 없습니다. 검증 수단이 없으니 결함이 들어와도 아무도 모르는 상태로 쌓였습니다.

### 조치 요약

| 결함 | 조치 | 상세 |
| --- | --- | --- |
| 시크릿 커밋 | 외부 파일 분리 + 환경변수 주입 + 템플릿 제공 | 4️⃣ |
| 인증 우회 | 평문 폴백 제거, 인코딩을 서비스 계층으로 이동 | 5️⃣ |
| 인가 부재 | URL 패턴별 RBAC + 리소스 소유권 검증 | 6️⃣ |
| 엔티티 노출 | Request/Response DTO 분리 + 직렬화 차단 | 5️⃣ |
| 검증 부재 | 인가·자격증명 회귀 테스트로 고정 (21개) | 1️⃣2️⃣ |

> 💬 **"인수받은 프로젝트에서 뭘 가장 먼저 봤나요?"**
> 개별 버그보다 "이 결함이 왜 여기까지 살아남았나"를 봤습니다. 시크릿 노출·인가 부재·자격증명 유출이 전부 자동 검증 대상이 아니었다는 공통점이 있었습니다. 그래서 고치는 것과 테스트로 고정하는 것을 한 세트로 진행했습니다.

### 참고: 빌드 실패 (구조적 결함은 아님)

인수 시점 `main`은 컴파일되지 않았습니다(에러 16건). 다만 이건 구조적 문제가 아니라 **마지막 커밋 하나**의 문제였습니다. 직전 커밋(`b66952a`)은 정상 빌드되고, 마지막 커밋(`cfd21eb`, 메시지 `"mi"`)이 `RoleKind`를 `String`에서 enum으로 바꾸는 작업을 5개 파일까지 하다가 나머지 호출부 16곳을 남긴 채 중단된 상태였습니다. 경계마다 명시적 변환을 넣어 정리했습니다.

---

## 2️⃣ 리플렉션으로 getter를 추측하던 코드 제거

GoCafe에는 **엔티티 필드명이 확정되지 않아 리플렉션으로 메서드를 찾아보는** 코드가 있었습니다.

```java
// Before (GoCafe ReviewService.java)
private String extractTagCode(ReviewTag t) {
    try { Object v = ReviewTag.class.getMethod("getTagCode").invoke(t); if (v!=null) return v.toString(); } catch (Exception ignored) {}
    try { Object v = ReviewTag.class.getMethod("getCode").invoke(t);    if (v!=null) return v.toString(); } catch (Exception ignored) {}
    try { Object v = ReviewTag.class.getMethod("getTag").invoke(t);     if (v!=null) return v.toString(); } catch (Exception ignored) {}
    return null;
}

/** ReviewTag에 '코드' 값을 안전하게 넣기 (setTagCode/setCode/setTag 중 있는 것 호출) */
private void setTagCodeSafely(ReviewTag t, String code) {
    try { ReviewTag.class.getMethod("setTagCode", String.class).invoke(t, code); return; } catch (Exception ignored) {}
    // ...
    // 위 세터가 아무것도 없다면, 프로젝트의 실제 필드명을 알려줘. 그에 맞게 한 줄만 바꿔 줄게.
}
```

세터가 하나도 없으면 **조용히 아무 일도 하지 않고 넘어갑니다.** 컴파일러가 잡아줄 수 있는 오류를 런타임 무음 실패로 바꾼 셈입니다.

**조치** — 태그 도메인을 타입으로 확정하고 리플렉션을 제거했습니다 (아래 3️⃣).

> 💬 **"왜 리플렉션이 문제인가요?"**
> 이 코드는 필드명을 바꿔도 컴파일 에러가 안 납니다. 대신 배포 후 태그가 조용히 저장되지 않습니다. 실패를 늦게, 조용히 만드는 코드라 가장 먼저 걷어냈습니다.

---

## 3️⃣ 태그 도메인 재설계 — CSV 문자열에서 정규화 + 타입 안전 enum으로

### Before: 비정규화된 CSV 컬럼

```java
// Before (GoCafe Review.java)
@Column(name = "liked_tag_csv")
private String likedTagCsv;      // "WIFI,STUDY,LATTE" 형태
```

CSV 컬럼은 **조회·집계·검증 어느 것도 DB가 도와주지 못합니다.** "와이파이 태그가 붙은 카페"를 찾으려면 `LIKE '%WIFI%'`밖에 방법이 없고, `WIFI2` 같은 오타도 그대로 저장됩니다.

태그 목록도 실제 연관관계가 아니라 `@Transient` 필드였습니다.

```java
// Before (GoCafe Review.java)
@Transient
private List<ReviewTag> tags;    // DB와 연결되지 않음
```

### After: 4개 카테고리 enum + 실제 연관관계

```java
public interface TagEnum {
    String name();
    String getLabel();          // "와이파이"  (화면 표시용)
    String getCategory();       // "FACILITY"  (분류 코드)
    String getCategoryLabel();  // "시설"
}

public enum FacilityTag implements TagEnum {
    PLUG("콘센트"), TERRACE("테라스"), PET("반려동물"), PARKING("주차"), WIFI("와이파이");
    @Override public String getCategory() { return "FACILITY"; }
}
```

`FacilityTag / MenuTag / PurposeTag / MoodTag` 4종이 같은 인터페이스를 구현해, **카테고리별 분기 없이 일괄 처리**할 수 있게 됐습니다.

```java
// 카테고리와 무관하게 동일 코드로 처리
private void saveTagDesc(TagEnum tag) {
    tagDictionaryRepository.save(TagDictionary.builder()
        .tagCategory(tag.getCategory())
        .tagEnum(tag.name())
        .tagLabel(tag.getLabel())
        .build());
}
```

엔티티도 문자열 코드에서 enum 컬럼으로 바꿨습니다.

```java
// Before (GoCafe CafeTag.java)
@Column(name = "category_code", length = 20) private String categoryCode;
@Column(name = "code",          length = 20) private String code;

// After (MyPickCafe CafeTag.java)
@Enumerated(EnumType.STRING) @Column(name = "facility_tag", length = 20) private FacilityTag facilityTag;
@Enumerated(EnumType.STRING) @Column(name = "menu_tag",     length = 20) private MenuTag     menuTag;
@Enumerated(EnumType.STRING) @Column(name = "purpose_tag",  length = 20) private PurposeTag  purposeTag;
@Enumerated(EnumType.STRING) @Column(name = "mood_tag",     length = 20) private MoodTag     moodTag;
```

### 복합 유니크 제약 오류 수정

```java
// Before (GoCafe) — 카테고리가 달라도 코드가 같으면 저장 불가
@UniqueConstraint(columnNames = {"review_id", "code"})

// After — 카테고리까지 포함해야 올바른 식별자
@UniqueConstraint(columnNames = {"review_id", "category_code", "code"})
```

예를 들어 `MENU=CAKE`와 `MOOD=CAKE`는 공존할 수 있어야 하는데 기존 제약이 막고 있었습니다.

---

## 4️⃣ 보안 ① 저장소에 커밋된 시크릿

```properties
# Before (GoCafe application.properties) — 저장소에 그대로 커밋됨
spring.datasource.password=jsp2025
app.jwt.secret=b+QWlwcbxeaY...(생략)...U3A=
```

DB 비밀번호와 **JWT 서명 키**가 Git 히스토리에 남아 있었습니다. JWT 시크릿 유출은 곧 **임의의 사용자로 토큰을 위조할 수 있다**는 뜻입니다.

```properties
# After — 저장소 밖으로 분리, 환경변수로도 주입 가능
spring.config.import=optional:file:./secret.properties
spring.datasource.password=${DB_PASSWORD}
app.jwt.secret=${JWT_SECRET}
```

`secret.properties`는 `.gitignore` 처리하고, 다른 사람이 바로 띄울 수 있도록 `secret.properties.example` 템플릿을 함께 제공했습니다.

> ⚠️ **말할 때 주의**
> "시크릿을 분리했다"보다 "JWT 키가 유출되면 토큰 위조가 가능하다"까지 설명해야 위험도를 이해하고 있다는 게 전달됩니다.

---

## 5️⃣ 보안 ② 인증 없이 비밀번호 해시가 노출되던 취약점

세 조건이 겹쳐 **인증 없이 전 회원의 이메일과 BCrypt 해시를 덤프**할 수 있었습니다.

```java
// (1) 엔티티를 그대로 응답으로 반환 (GoCafe MemberApiController)
@GetMapping
public List<Member> getAll() { return service.findAll(); }

// (2) password에 직렬화 차단이 없음 (GoCafe Member.java)
@Column(name = "password", nullable = false, length = 100)
private String password;                    // ← @JsonIgnore 없음

// (3) 해당 경로가 인증 없이 열려 있음 (GoCafe SecurityConfig)
.requestMatchers(HttpMethod.GET,  "/api/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/**").permitAll()
```

같은 문제가 **연관관계를 타고** 두 경로에서 더 재현됐습니다. `Cafe.owner`와 `Menu.cafe`에도 직렬화 차단이 없었기 때문입니다.

| 경로 | 유출 경로 | 인증 |
| --- | --- | --- |
| `GET /api/members` | `Member.password` 직접 | 불필요 |
| `GET /api/cafes` | `Cafe.owner` → `password` | 불필요 |
| `GET /api/menus/by-cafe/{id}` | `Menu.cafe` → `owner` → `password` | 불필요 |

### 조치 (다중 방어)

1. **DTO 분리** — `MemberResponse` / `CafeResponse` / `MenuResponse`로 응답 필드를 화이트리스트 방식으로 명시
2. **엔티티 레벨 차단** — 엔티티가 직렬화되더라도 새지 않도록 `@JsonProperty(access = WRITE_ONLY)` 적용 (역직렬화만 허용해 폼 바인딩은 유지)
3. **인가 강화** — `/api/members` 이하를 ADMIN 전용으로 제한. 단, 이 규칙은 와일드카드 permitAll 규칙보다 **먼저** 선언해야 한다 (Spring Security는 최초 매칭 규칙이 이기므로 **순서가 곧 보안**)
4. **회귀 테스트로 고정** — 아래 1️⃣2️⃣ 참고

### 덤으로 발견한 인증 우회 경로

```java
// Before (GoCafe AuthApiController) — 해시 불일치 시 평문 비교로 폴백
if (!matches) matches = pw.equals(member.getPassword()); // dev only
```

평문 비밀번호가 저장된 계정이 하나라도 생기면 그대로 로그인이 뚫립니다. 실제로 회원 생성 API가 **비밀번호를 인코딩하지 않고 저장**하고 있어 두 결함이 연결될 수 있었습니다. 폴백을 제거하고, 인코딩을 서비스 계층으로 옮겨 평문이 저장될 경로 자체를 없앴습니다.

> 💬 **"엔티티를 API에 그대로 쓰면 왜 안 되나요?"**
> 교과서적으로는 "계층 결합"이지만, 제 프로젝트에서는 실제로 비밀번호 해시가 인증 없이 노출되는 취약점이 됐습니다. 엔티티는 DB 스키마를 표현하는 모델이지 API 계약이 아니고, 연관관계를 타고 의도치 않은 데이터까지 따라 나갑니다.

---

## 6️⃣ 인가 체계 재설계

```java
// Before (GoCafe) — 사실상 인가가 없음
.requestMatchers(HttpMethod.GET,  "/api/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/**").permitAll()
.requestMatchers("/h2-console/**").permitAll()
```

POST까지 전부 열려 있어 **인가 판단이 각 컨트롤러 내부로 흩어져** 있었고, 누락되면 그대로 보안 구멍이었습니다.

```java
// After — 역할별 URL 패턴을 선언적으로 강제
.requestMatchers("/api/members/**").hasRole("ADMIN")
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/cafes/new", "/cafes/create").hasAnyRole("CAFEOWNER", "ADMIN")
.requestMatchers("/api/menus/**").hasAnyRole("CAFEOWNER", "ADMIN")
.requestMatchers("/reviews/**", "/favorites/**", "/member/**").authenticated()
.anyRequest().authenticated()
```

### 요청 유형별 인증 실패 응답 분기

같은 서버가 REST API와 Mustache 페이지를 함께 서빙하므로 응답이 달라야 했습니다. API는 401 JSON, 브라우저는 로그인 페이지로 리다이렉트합니다.

```java
private boolean isApiRequest(HttpServletRequest req) {
    if (req.getRequestURI().startsWith("/api/")) return true;
    String accept = req.getHeader("Accept");
    return accept != null && accept.contains("application/json");
}
```

### 리소스 단위 소유권 검증 (IDOR 방지)

URL 인가는 "CAFEOWNER 역할인가"까지만 판단합니다. "**이** 카페의 점주인가"는 리소스를 조회해야 알 수 있어, 검증이 없으면 점주 A가 점주 B의 메뉴를 수정할 수 있었습니다. 여러 컨트롤러에 중복돼 있던 로직을 `CafeOwnershipGuard`로 모으고 메뉴 API에도 적용했습니다.

---

## 7️⃣ 비즈니스 로직 정정 — 점주 승격 시점

```java
// Before (GoCafe CafeService) — 카페 "등록" 시점에 승격 + 타입 오류
String role = cafeOwner.getRoleKind();
if (role == null || !"owner".equalsIgnoreCase(role)) {
    cafeOwner.setRoleKind("owner");          // "owner"는 RoleKind에 없는 값
    memberRepository.save(cafeOwner);
}
```

두 가지가 잘못돼 있었습니다.

1. **타입 오류** — 컴파일 에러 (1️⃣의 16건 중 2건)
2. **로직 오류** — 카페를 *등록만 해도* 점주가 됩니다. 관리자 승인 절차(`PENDING → APPROVED`)가 있는데 무의미해집니다.

```java
// After — 관리자가 승인한 시점에만, 일반 회원인 경우에만 승격
@Transactional
public void approve(Long cafeId) {
    Cafe c = cafeRepository.findById(cafeId).orElseThrow(...);
    c.setStatus(CafeStatus.APPROVED);
    Member owner = c.getOwner();
    if (owner != null && owner.getRoleKind() == RoleKind.MEMBER) {
        owner.setRoleKind(RoleKind.CAFEOWNER);
        memberRepository.save(owner);
    }
}
```

또한 역할 이름을 `PRO`(프로)에서 `CAFEOWNER`(카페 사장)로 바꿔 **도메인 용어와 코드 용어를 일치**시켰습니다.

> 💬 **"리팩토링 중 발견한 버그는?"**
> 컴파일 에러를 고치다 발견했습니다. 타입만 맞춰서 넘어갈 수도 있었지만 "왜 등록 시점에 승격하지?"를 따져보니 승인 프로세스를 우회하는 로직이었습니다. 컴파일 에러가 오히려 도메인 규칙을 다시 보게 만든 사례입니다.

---

## 8️⃣ 스키마 정리 — 예약어와 실측 기반 길이

| 항목 | Before (GoCafe) | After (MyPickCafe) | 이유 |
| --- | --- | --- | --- |
| 전화번호 컬럼 | `number` | `phone` | 의미가 불명확한 이름 |
| 등록일 컬럼 | `date` | `registered_at` | `date`는 SQL 예약어 |
| 카페명 길이 | 10자 | 30자 | 실데이터에 부족 |
| 주소 길이 | 60자 | 100자 | 실데이터에 부족 |
| 위경도 | `nullable = false` | nullable 허용 | 좌표 없이도 등록 가능해야 함 |

GoCafe는 Oracle에서 예약어 문제를 `globally_quoted_identifiers=true`(모든 식별자를 따옴표로 감싸기)로 우회하고 있었습니다. PostgreSQL로 옮기면서 그 옵션을 제거하고 **이름 자체를 고쳤습니다.**

연관관계 6개에는 `@JsonIgnore`를 붙여 순환 참조와 과다 직렬화를 차단했습니다.

---

## 9️⃣ DB 마이그레이션 — Oracle + H2 → PostgreSQL (Docker)

GoCafe는 Oracle(`ojdbc11`)에 H2까지 물려 있어 팀원마다 동작이 달랐습니다. `ojdbc11`을 제거하고 dialect를 전환한 뒤, `docker-compose.yml`로 PostgreSQL 16을 컨테이너화해 **환경 재현성**을 확보했습니다.

**실제로 터진 이슈** (커밋 `b357764`)

| 이슈 | Oracle | PostgreSQL | 조치 |
| --- | --- | --- | --- |
| boolean | `NUMBER(1)` + `= 1` 비교 | 네이티브 `BOOLEAN` | 네이티브 쿼리 비교식 수정 |
| 식별자 | 따옴표 대문자 | 기본 소문자 폴딩 | 테이블/컬럼명 정리 |
| SQL 문법 | `SET DEFINE OFF` 등 | 불필요 | 더미 SQL 재작성 |

> 💬 **"마이그레이션에서 가장 어려웠던 점은?"**
> JPA로 추상화된 부분은 dialect 교체로 끝났지만, 네이티브 쿼리는 자동 변환이 안 돼서 직접 찾아 고쳐야 했습니다. 이 경험으로 네이티브 쿼리가 벤더 종속성을 만든다는 걸 체감했습니다.

---

## 🔟 신규 구축 — AI 기반 태그 추출과 자동 재집계

GoCafe의 태그는 수기 등록이었고, 리뷰 태그를 카페 단위로 모으는 로직도 없었습니다.

### 리뷰 → 태그·감성 자동 추출

FastAPI(Ollama `qwen2.5:7b`) 연동을 **전용 Client 계층**으로 감싸 Service/Controller가 HTTP 세부사항을 모르게 했습니다.

### 장애 격리 (Graceful Degradation)

AI는 **부가 기능**이므로 죽어도 리뷰 작성은 성공해야 합니다.

```java
Review saved = reviewRepository.save(review);      // 1) 저장 먼저 (필수)
pythonTagClient.analyze(pyReq).ifPresent(res -> {  // 2) 태그는 부가 (실패 시 skip)
    saveEnumTags(saved, res);
    syncCafeTopTags(saved.getCafe().getId());
});
```

**저장 순서 자체가 설계 의도**입니다. AI 응답을 기다렸다 저장하면 AI 장애가 곧 리뷰 기능 장애가 됩니다.

기존 `RestTemplate`은 **타임아웃이 무제한**이라 AI 서버가 응답을 붙잡으면 요청 스레드가 묶였습니다. WebClient로 전환하며 연결 2초 / 응답 10초를 명시했습니다.

### 카페 대표 태그 자동 재집계

```sql
SELECT t.category_code, t.code, COUNT(*) AS cnt
  FROM review_tag t JOIN review r ON r.review_id = t.review_id
 WHERE r.cafe_id = :cafeId AND r.sentiment = 'GOOD'   -- 긍정 리뷰만 반영
 GROUP BY t.category_code, t.code
 ORDER BY t.category_code, cnt DESC
```

상위 N개를 자르는 대신 **1위 대비 85% 이상**인 태그를 채택했습니다.

```java
long topCount = ((Number) tagCounts.get(0)[2]).longValue();
for (Object[] tagCount : tagCounts) {
    if (((Number) tagCount[2]).longValue() < topCount * 0.85) break;
    // ... CafeTag 저장
}
```

**왜 Top-N이 아니라 상대 임계값인가?**

| 방식 | 문제 |
| --- | --- |
| Top-3 고정 | 10표/9표/9표/9표일 때 동률인 4번째가 부당하게 탈락 |
| 절대 임계값 (5표 이상) | 리뷰 적은 신규 카페는 태그가 아예 안 붙음 |
| **상대 임계값 (채택)** | 리뷰 수와 무관하게 "압도적인 태그만" 선별 → 두 문제 동시 해결 |

리뷰 **작성 / 수정 / 삭제** 세 시점 모두에 연결했습니다. 삭제 시에는 `cafeId`를 미리 꺼내 둡니다 — 삭제 후에는 연관 엔티티에 접근할 수 없기 때문입니다.

---

## 1️⃣1️⃣ 운영 관점 — 프로파일 분리와 N+1 대응

GoCafe는 단일 파일에 개발용 설정이 그대로 있어, 배포하면 스택트레이스가 노출되는 구성이었습니다.

| 설정 | GoCafe | dev | prod |
| --- | --- | --- | --- |
| `include-stacktrace` | `ALWAYS` | `ALWAYS` | `never` |
| `ddl-auto` | `update` | `update` | `validate` |
| Security 로깅 | `DEBUG` | `DEBUG` | `WARN` |
| 쿠키 `Secure` | `false` | `false` | `true` |
| CORS | 코드에 와일드카드 하드코딩 | 와일드카드 | 환경변수 화이트리스트 |

특히 prod의 **`ddl-auto=validate`**가 핵심입니다. `update`는 운영 스키마를 자동 변경해 데이터 손상 위험이 있고, `validate`는 엔티티와 스키마가 어긋나면 **기동 자체를 중단**시킵니다.

성능 쪽으로는 목록 화면의 N+1에 대응해 배치 페치를 켰습니다 (GoCafe에는 없던 설정).

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=100
```

> 💬 **"fetch join을 쓰지 않은 이유는?"**
> fetch join은 컬렉션이 둘 이상이면 `MultipleBagFetchException`이 나고 페이징과 함께 쓰면 메모리 페이징 문제가 생깁니다. batch size는 전역 적용되고 페이징과 안전하게 공존해 목록 화면에는 이쪽이 적합했습니다.

---

## 1️⃣2️⃣ 테스트 — 개선 사항을 회귀 테스트로 고정

GoCafe는 `contextLoads()` 하나뿐이었고, 그마저 로컬 DB가 없으면 실패했습니다. H2 인메모리 `test` 프로파일을 추가해 **Docker 없이도 전체 스위트가 실행**됩니다.

**21개 테스트 / 실패 0**

| 테스트 | 검증 대상 | 개수 |
| --- | --- | --- |
| `ApiAuthorizationTest` | 역할별 인가 규칙 (비로그인/MEMBER/ADMIN) | 7 |
| `MemberResponseLeakTest` | **비밀번호 해시 미노출** (회원·카페 두 경로) | 2 |
| `MemberServiceTest` | 비밀번호 인코딩, 중복 검증, null 필드 무시 | 5 |
| `CafeServiceTest` | 승인 상태 강제, 소유자 지정, 중복 거부 | 3 |
| `AiClientDegradationTest` | AI 서버 장애 시 graceful degradation | 3 |
| `MyPickCafeApplicationTests` | 컨텍스트 로드 | 1 |

```java
// 취약점 회귀 방지 — 응답 어디에도 해시가 없어야 한다
assertThat(body).doesNotContain(BCRYPT_HASH).doesNotContain("$2a$");
assertThat(body).contains("leak-test-owner");   // 필요한 데이터는 남아있음(단언 유효성 확인)

// 장애 격리 — 죽은 포트로 실제 호출해 실패 경로를 그대로 태운다
PythonTagClient client = new PythonTagClient(WebClient.create("http://localhost:19999"));
assertThat(client.analyze(request)).isEmpty();
```

> 💬 **"테스트는 어떤 기준으로 작성했나요?"**
> 커버리지 숫자보다 회귀하면 치명적인 것부터 고정했습니다. 인가 규칙과 자격증명 노출은 코드만 봐서는 안전한지 확신할 수 없고 실수하면 바로 사고로 이어져, 테스트로 못 박는 게 가장 효과적이라고 판단했습니다.

---

## 1️⃣3️⃣ 스코프 정리 — 덜어낸 것들

리팩토링이 더하는 것만은 아니라는 점을 보여줄 수 있는 부분입니다.

| 제거 대상 | 이유 |
| --- | --- |
| `ProGateService`, `MissionController`, `AdminMissionController` | 리뷰 10건·GOOD 6건이면 PRO로 승격하는 게이미피케이션. 미완성이고 컴파일 에러의 원인 중 하나 |
| `/api/cafe-tags`, `/api/categories`, `/api/needs` | 프론트엔드에서 호출되지 않으면서 엔티티를 노출하던 CRUD 스캐폴딩 |
| `MenuController` | `MenuApiController`와 동일 경로에 중복 매핑 |
| `Review.waitingTime / companionType / taste` | 화면 어디서도 쓰지 않던 컬럼 |

> 💬 **"코드를 지울 때 기준은?"**
> 호출부를 먼저 확인합니다. 템플릿·정적 리소스·자바 코드를 모두 grep해서 참조가 없거나 대체 경로가 있는 것만 지웠습니다. 미사용 엔드포인트는 유지비만 드는 게 아니라 공격 표면이라 지우는 쪽이 안전합니다.

---

## 1️⃣4️⃣ 개발 프로세스

| 항목 | GoCafe | MyPickCafe |
| --- | --- | --- |
| 커밋 메시지 | `commit001`, `commit002: oracle error`, `Mypage design Changi`(오타 반복) | `회원 비밀번호 해시가 인증 없이 노출되던 취약점 차단` |
| 브랜치 | 개인명 (`HsH`, `Jin`, `YG`, `Changi`) | 기능 단위 (`fix/member-credential-exposure` 등) |
| 브랜치 접두사 | 없음 | `fix/` `refactor/` `feat/` `chore/` `test/` `docs/`로 변경 성격 구분 |
| 문서 | `readme.md` 1줄 | `SETUP.md`, `PORTFOLIO.md`, 모델 설정 비교 실험 기록 |

커밋 본문에 **"왜"를 남깁니다.** 예를 들어 취약점 커밋에는 세 가지 성립 조건과 각각의 대응을 적어, 나중에 그 커밋만 봐도 판단 근거가 복원됩니다.

---

## 🎯 면접 예상 질문 대비 요약

| 질문 | 핵심 답변 | 섹션 |
| --- | --- | --- |
| **가장 임팩트 있었던 작업은?** | **인증 없이 비밀번호 해시가 노출되던 취약점 발견·차단** | 5️⃣ |
| **인수받은 프로젝트에서 뭘 먼저 봤나?** | 개별 버그보다 "이 결함이 왜 살아남았나" — 검증 수단 부재가 공통 원인 | 1️⃣ |
| 토큰 보안 이슈는? | JWT 서명 키가 저장소에 커밋돼 인증 체계 자체가 무력화 | 1️⃣ |
| 리팩토링 중 발견한 버그는? | 점주 승격이 승인 절차를 우회 / 복합 유니크 제약 오류 | 7️⃣, 3️⃣ |
| 엔티티를 API에 쓰면 왜 안 되나? | 연관관계를 타고 자격증명이 실제로 유출됨 | 5️⃣ |
| 데이터 모델링 개선 사례는? | CSV 컬럼 → 정규화 테이블 + 타입 안전 enum | 3️⃣ |
| 설계 판단을 내린 사례는? | Top-N 대신 상대 임계값(85%) 채택 | 🔟 |
| 외부 API 장애 대응은? | 저장 우선 + Optional 반환 + 타임아웃 명시 | 🔟 |
| 성능 이슈 경험은? | 목록 조회 N+1 → batch fetch size | 1️⃣1️⃣ |
| DB 마이그레이션 난점은? | 네이티브 쿼리는 자동 변환이 안 됨 | 9️⃣ |
| 운영 배포를 고려한 부분은? | 프로파일 분리, prod `ddl-auto=validate` | 1️⃣1️⃣ |
| 테스트 기준은? | 회귀하면 치명적인 것(인가·자격증명)부터 | 1️⃣2️⃣ |
| 코드를 지울 때 기준은? | 호출부 확인 후, 미사용 엔드포인트는 공격 표면으로 간주 | 1️⃣3️⃣ |
| 더 개선한다면? | CI/CD, OSIV 정리, 재집계 로직 테스트 | 아래 |

---

## 📋 남은 과제 (솔직하게 말할 거리)

- **CI/CD 부재** — GitHub Actions로 빌드·테스트 자동화 필요
- **`spring.jpa.open-in-view` 기본값(true) 유지** — API 경로는 서비스 계층에서 DTO 매핑을 끝냈지만 뷰 렌더링 경로는 아직 OSIV에 의존
- **N+1 추가 최적화** — `CafeResponse` 매핑 시 `owner` 조회가 batch fetch에 의존. fetch join 전용 쿼리로 더 줄일 여지
- **테스트 확대** — 태그 재집계(85% 임계값) 로직 단위 테스트 미작성
- **`CategoryService`** — 스캐폴딩 컨트롤러 삭제로 현재 미사용 (정리 대상)
- **실제 기동 검증** — 현재 검증 범위는 clean build와 H2 기반 테스트까지. PostgreSQL을 띄운 상태의 E2E 확인은 별도 필요
