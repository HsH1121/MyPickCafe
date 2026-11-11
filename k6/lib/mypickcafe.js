// MyPickCafe 부하 테스트 공통 코드 — 사용자 한 명이 사이트를 둘러보는 흐름과 요청 도우미.
// smoke.js · load.js · stress.js 가 이 파일을 가져다 쓰고, VU 수와 생각하는 시간만 다르게 줍니다.
//
// 원칙
//   - 읽기만 합니다. 리뷰·찜·카페 등록처럼 데이터를 바꾸는 요청은 보내지 않습니다.
//     (테스트 데이터가 DB 에 쌓이고, 리뷰 작성은 AI 서버 호출까지 따라옵니다.)
//   - 픽봇(/api/pickbot/recommend)은 넣지 않았습니다. 요청마다 외부 LLM(Fireworks)을 불러 비용이 들고,
//     응답이 수 초~수십 초라 앱이 아니라 LLM 의 응답 시간을 재게 됩니다.
//   - 요청마다 name 태그를 붙입니다. /cafes/12, /cafes/13 처럼 주소가 달라도 같은 화면으로 묶어
//     터미널 요약과 Grafana 에서 화면별로 봅니다.

import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');

const USER_EMAIL = __ENV.K6_USER_EMAIL || '';
const USER_PASSWORD = __ENV.K6_USER_PASSWORD || '';
/** 로그인 사용자 시나리오를 돌릴 수 있는지(.env 의 K6_USER_EMAIL·K6_USER_PASSWORD). */
export const HAS_MEMBER = USER_EMAIL !== '' && USER_PASSWORD !== '';

/**
 * 결과에 붙일 기본 태그. url 은 뺍니다. 카페마다 주소가 달라 Prometheus 에 카페 수만큼 시계열이 생기기 때문입니다
 * (화면 구분은 name 태그로 합니다).
 */
export const SYSTEM_TAGS = [
  'status', 'method', 'name', 'group', 'check', 'error', 'error_code', 'scenario', 'expected_response',
];

/** 터미널 요약에 p(99)까지 보이게 합니다. */
export const SUMMARY_TREND_STATS = ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'];

// 검색어: 이름·주소에 들어가는 지역명과 일반 단어. 결과가 많고 적은 경우가 섞입니다.
const SEARCH_WORDS = ['홍대', '연남', '망원', '성수', '강남', '이태원', '합정', '신사', '카페', '커피'];
// 태그 필터: CafeController 의 "카테고리:코드" 형식(domain 의 *Tag enum)
const TAG_FILTERS = [
  'FACILITY:WIFI', 'FACILITY:PLUG', 'FACILITY:PARKING',
  'PURPOSE:STUDY', 'PURPOSE:DATE', 'MOOD:MODERN', 'MENU:AMERICANO',
];

const pick = (list) => list[Math.floor(Math.random() * list.length)];

/**
 * GET 요청 하나와 200 검사. 리다이렉트는 따라가지 않습니다.
 * (로그인이 풀려 /login 으로 보내지면 302 로 남아 실패로 잡히게 하려는 것)
 */
function get(path, name, headers) {
  const res = http.get(`${BASE_URL}${path}`, { headers, redirects: 0, tags: { name } });
  check(res, { [`${name} 200`]: (r) => r.status === 200 });
  return res;
}

/**
 * 테스트 시작 전에 한 번(setup) 실행합니다. 승인된 카페 ID 를 받아 두고, 로그인 계정이 맞는지 확인합니다.
 * 데이터나 계정이 없으면 수백 명을 보내 봐야 의미가 없으니 바로 멈춥니다.
 */
export function prepare() {
  const res = http.get(`${BASE_URL}/api/cafes`, { tags: { name: 'setup' } });
  if (res.status !== 200) {
    fail(`GET ${BASE_URL}/api/cafes 가 HTTP ${res.status} 입니다. 앱이 떠 있는지, BASE_URL 이 맞는지 확인하세요.`);
  }
  const cafeIds = res.json().map((c) => c.id);
  if (cafeIds.length === 0) {
    fail('승인된 카페가 0개입니다. DB 에 데이터를 먼저 넣으세요(docs/MONITORING.md 의 "데이터 준비").');
  }

  if (HAS_MEMBER && !login('setup')) {
    // 틀린 비밀번호로 VU 마다 로그인하면 로그인 제한(LoginAttemptService)에 걸려 429 만 쌓입니다.
    fail(`K6_USER_EMAIL(${USER_EMAIL}) 로그인에 실패했습니다. .env 의 계정·비밀번호를 확인하세요.`);
  }
  console.log(`대상 ${BASE_URL}, 승인된 카페 ${cafeIds.length}개, 로그인 시나리오 ${HAS_MEMBER ? '켜짐' : '꺼짐(K6_USER_EMAIL 없음)'}`);
  return { cafeIds };
}

/**
 * POST /api/auth/login 으로 JWT 를 받습니다. 실패하면 null.
 * prod 프로파일의 인증 쿠키(AT)는 Secure 라 HTTP 로는 다시 보내지지 않으므로, 이후 요청은 Authorization 헤더를 씁니다.
 */
export function login(name = 'login') {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: USER_EMAIL, password: USER_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name } },
  );
  const ok = check(res, { [`${name} 200`]: (r) => r.status === 200 });
  return ok ? res.json('token') : null;
}

/**
 * 비로그인 사용자 한 명이 둘러보는 흐름.
 * 메인 → 카페 목록(정렬·태그 중 하나) → 카페 상세 1~3곳 → 가끔 검색.
 * think() 는 화면을 읽는 시간입니다(load 는 1~3초, stress 는 0초).
 */
export function browseAsGuest(data, think) {
  get('/', 'main');
  think();

  const r = Math.random();
  if (r < 0.5) {
    get('/cafes', 'cafe-list(likes)'); // 기본 정렬. 승인된 카페 전체와 리뷰 감성 집계를 읽는 가장 무거운 목록
  } else if (r < 0.75) {
    get('/cafes?sort=newest', 'cafe-list(newest)');
  } else {
    get(`/cafes?sort=tags&tag=${encodeURIComponent(pick(TAG_FILTERS))}`, 'cafe-list(tag)');
  }
  think();

  const details = 1 + Math.floor(Math.random() * 3);
  for (let i = 0; i < details; i++) {
    get(`/cafes/${pick(data.cafeIds)}`, 'cafe-detail'); // 카페 정보·사진·메뉴·리뷰 전체
    think();
  }

  if (Math.random() < 0.3) {
    get(`/search?q=${encodeURIComponent(pick(SEARCH_WORDS))}`, 'search');
    think();
  }
}

/**
 * 로그인 사용자 한 명이 둘러보는 흐름.
 * 화면을 열 때마다 머리글 스크립트가 부르는 /api/auth/me 와 알림 개수도 같이 보냅니다(브라우저와 같게).
 */
export function browseAsMember(data, token, think) {
  const auth = { Authorization: `Bearer ${token}` };
  const header = () => {
    get('/api/auth/me', 'api-me', auth);
    get('/api/notifications/unread-count', 'api-unread-count', auth);
  };

  get('/', 'main(member)', auth); // 니즈 기반 자카르드 추천 포함
  header();
  think();

  get('/cafes?sort=recommend', 'cafe-list(recommend)', auth);
  header();
  think();

  const details = 1 + Math.floor(Math.random() * 2);
  for (let i = 0; i < details; i++) {
    get(`/cafes/${pick(data.cafeIds)}`, 'cafe-detail', auth);
    header();
    think();
  }

  get('/api/favorites', 'api-favorites', auth);
  think();
}

/**
 * 스모크 테스트용: 위 두 흐름에 나오는 모든 화면을 무작위 없이 한 번씩 엽니다.
 * token 이 없으면 로그인 사용자 화면은 건너뜁니다.
 */
export function visitEveryPage(data, token, think) {
  get('/', 'main');
  get('/cafes', 'cafe-list(likes)');
  get('/cafes?sort=newest', 'cafe-list(newest)');
  get(`/cafes?sort=tags&tag=${encodeURIComponent(pick(TAG_FILTERS))}`, 'cafe-list(tag)');
  get(`/cafes/${pick(data.cafeIds)}`, 'cafe-detail');
  get(`/search?q=${encodeURIComponent(pick(SEARCH_WORDS))}`, 'search');
  think();
  if (!token) return;

  const auth = { Authorization: `Bearer ${token}` };
  get('/', 'main(member)', auth);
  get('/cafes?sort=recommend', 'cafe-list(recommend)', auth);
  get(`/cafes/${pick(data.cafeIds)}`, 'cafe-detail', auth);
  get('/api/auth/me', 'api-me', auth);
  get('/api/notifications/unread-count', 'api-unread-count', auth);
  get('/api/favorites', 'api-favorites', auth);
  think();
}

/** 단계(stages)의 목표 VU 에 비율을 곱합니다. 비로그인/로그인 사용자를 나눌 때 씁니다. */
export function scaleStages(stages, ratio) {
  return stages.map((s) => ({ duration: s.duration, target: Math.round(s.target * ratio) }));
}
