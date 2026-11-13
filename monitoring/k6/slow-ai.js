// 느린 AI 테스트 — AI 응답이 느릴 때(픽봇 추천·리뷰 작성), AI 를 부르지 않는 열람 화면까지 느려지는지 봅니다.
// 실제 AI 서버 대신 가짜 AI 서버(monitoring/ai-stub)를 붙여 돌립니다. LLM 을 부르지 않아 비용이 들지 않습니다.
//   docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/slow-ai.js
//   (리뷰 작성) docker compose run --rm -e SLOW_TARGET=review k6 run -o experimental-prometheus-rw /scripts/slow-ai.js
// 가짜 AI 서버 붙이기·되돌리기·리뷰 정리는 docs/MONITORING.md 의 "느린 AI 테스트". 약 7분 걸립니다.
//
// 왜 보는가
//   Spring 은 AI 응답을 Tomcat 스레드에서 기다립니다(WebClient ... .block()). 그동안 DB 커넥션까지 잡고 있으면
//   커넥션 풀(prod 20개)이 느린 요청에 묶여, AI 를 부르지 않는 화면도 커넥션을 기다리게 됩니다.
//   - 리뷰 작성: ReviewService.saveWithTags 가 @Transactional 이라 태그 분석을 기다리는 동안 커넥션을 잡습니다.
//   - 픽봇: spring.jpa.open-in-view 설정이 없어 기본값(켜짐)입니다. 승인된 카페 ID 를 조회한 뒤
//           AI 응답이 올 때까지 커넥션을 잡고 있을 것으로 봅니다. 이 테스트로 확인합니다.
//
// 구성
//   browse : 비로그인 BROWSE_VUS(기본 40)명이 load.js 와 같은 흐름으로 처음부터 끝까지 둘러봅니다. 판정 대상입니다.
//   slow   : 1분 30초 뒤부터 느린 요청을 SLOW_STEPS(기본 5,10,20,30)건으로 늘립니다. 단계마다 1분 유지.
//            VU 하나가 느린 요청을 하나 보내고, 응답을 받으면 1초 쉬고 다시 보냅니다. VU 수 ≈ 동시에 걸린 AI 호출 수.
//            SLOW_TARGET=pickbot(기본): 비로그인 픽봇 추천
//            SLOW_TARGET=review       : 리뷰 작성. K6_USER_EMAIL 계정이 필요하고 리뷰가 실제로 쌓이므로 끝나고 지웁니다.
//
// 판정: 열람(browse) 요청의 p95 500ms·실패 1% 미만(load.js 와 같은 기준)을 시간 구간(step 태그)마다 따로 봅니다.
//   baseline(느린 요청 없음) → slow05 → slow10 → slow20 → slow30 → cooldown(느린 요청을 뺀 뒤)
//   요약의 http_req_duration{scenario:browse,step:...} 줄에서 처음 ✗ 가 나오는 단계가
//   "느린 AI 호출이 사이트 전체를 끌어내리는" 동시 호출 수입니다. 하나라도 어기면 종료 코드 99 입니다.
import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';
import {
  BASE_URL, SUMMARY_TREND_STATS, SYSTEM_TAGS,
  browseAsGuest, pick, prepare,
} from './lib/mypickcafe.js';

const TARGET = (__ENV.SLOW_TARGET || 'pickbot').toLowerCase();
if (TARGET !== 'pickbot' && TARGET !== 'review') {
  throw new Error(`SLOW_TARGET 은 pickbot 또는 review 입니다(받은 값: ${TARGET}).`);
}
const BROWSE_VUS = Number(__ENV.BROWSE_VUS || 40);
const STEPS = (__ENV.SLOW_STEPS || '5,10,20,30').split(',').map((s) => Number(s.trim())).filter((n) => n > 0);

// 시간표(초). 단계 사이 RAMP 구간은 판정에서 뺍니다(VU 가 늘어나는 중이라 어느 단계인지 모호).
const WARMUP = 30;      // browse 가 BROWSE_VUS 까지 늘어나는 시간
const SLOW_START = 90;  // 그 뒤 1분은 느린 요청 없이 기준(baseline)을 잽니다
const RAMP = 10;
const HOLD = 60;
const SLOW_END = SLOW_START + STEPS.length * (RAMP + HOLD) + RAMP; // 느린 요청을 0 으로 줄이기까지
const COOLDOWN = 30;    // 느린 요청을 뺀 뒤 회복되는지

const stepName = (n) => `slow${String(n).padStart(2, '0')}`;
const JUDGED_STEPS = ['baseline', ...STEPS.map(stepName), 'cooldown'];

// 리뷰 작성 모드에서 쌓인 리뷰를 찾아 지우는 표시(docs/MONITORING.md 의 정리 SQL)
const REVIEW_MARK = '[k6-slow-ai]';
const PICKBOT_QUERIES = [
  '조용한 카페', '공부하기 좋은 카페', '디저트 맛있는 곳', '홍대 데이트 카페', '콘센트 많은 카페', '주차 되는 카페',
];
// Spring 의 픽봇 대기 한도(ai.pickbot.read-timeout-ms=60초)보다 길게 둬서, k6 가 먼저 끊지 않고 Spring 의 응답을 봅니다.
const SLOW_REQUEST_TIMEOUT = '90s';

/** 테스트 시작부터 지난 초. slow 시나리오는 SLOW_START 뒤에 시작하므로 그만큼 더합니다. */
function elapsedSec() {
  const offset = exec.scenario.name === 'slow' ? SLOW_START : 0;
  return (Date.now() - exec.scenario.startTime) / 1000 + offset;
}

/** 지금이 시간표의 어느 구간인지. */
function currentStep() {
  const t = elapsedSec();
  if (t < WARMUP) return 'warmup';
  if (t < SLOW_START) return 'baseline';
  for (let i = 0; i < STEPS.length; i++) {
    const holdStart = SLOW_START + i * (RAMP + HOLD) + RAMP;
    if (t < holdStart) return 'ramp';
    if (t < holdStart + HOLD) return stepName(STEPS[i]);
  }
  return t < SLOW_END ? 'ramp' : 'cooldown';
}

/** 이 VU 가 보내는 요청에 지금 구간을 step 태그로 붙입니다. */
function tagStep() {
  exec.vu.metrics.tags.step = currentStep();
}

const slowStages = [];
for (const n of STEPS) {
  slowStages.push({ duration: `${RAMP}s`, target: n }, { duration: `${HOLD}s`, target: n });
}
slowStages.push({ duration: `${RAMP}s`, target: 0 });

const thresholds = {};
for (const s of JUDGED_STEPS) {
  thresholds[`http_req_duration{scenario:browse,step:${s},expected_response:true}`] = ['p(95)<500'];
  thresholds[`http_req_failed{scenario:browse,step:${s}}`] = ['rate<0.01'];
}
// 느린 요청 쪽은 판정하지 않고 단계별 값을 요약에 보이게만 합니다(응답 시간 = AI 지연 + 앞에서 기다린 시간).
for (const s of STEPS.map(stepName)) {
  thresholds[`http_req_duration{scenario:slow,step:${s}}`] = ['max>=0'];
  thresholds[`http_req_failed{scenario:slow,step:${s}}`] = ['rate>=0'];
}

export const options = {
  scenarios: {
    browse: {
      executor: 'ramping-vus',
      exec: 'browse',
      startVUs: 0,
      stages: [
        { duration: `${WARMUP}s`, target: BROWSE_VUS },
        { duration: `${SLOW_END + COOLDOWN - WARMUP}s`, target: BROWSE_VUS },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
    slow: {
      executor: 'ramping-vus',
      exec: 'slow',
      startTime: `${SLOW_START}s`,
      startVUs: 0,
      stages: slowStages,
      // 줄어드는 VU 의 요청이 끝나기를 기다립니다(중간에 끊으면 AI 대기가 덜 잡힘).
      gracefulRampDown: SLOW_REQUEST_TIMEOUT,
      gracefulStop: SLOW_REQUEST_TIMEOUT,
    },
  },
  thresholds,
  systemTags: SYSTEM_TAGS,
  summaryTrendStats: SUMMARY_TREND_STATS,
  // setup 의 가짜 AI 확인이 픽봇 한 번(기본 8초, 최대 60초)을 기다립니다.
  setupTimeout: '120s',
};

/**
 * 가짜 AI 서버가 붙어 있는지 확인합니다. 실제 AI 서버에 이 테스트를 돌리면 LLM 호출이 수백 번 나가 비용이 듭니다.
 * 가짜 AI 서버는 추천 결과의 카페 이름을 "(가짜 AI)" 로 시작해 줍니다.
 */
function assertStubAttached() {
  const res = http.post(`${BASE_URL}/api/pickbot/recommend`, JSON.stringify({ query: '가짜 AI 확인' }), {
    headers: { 'Content-Type': 'application/json' }, tags: { name: 'setup' }, timeout: SLOW_REQUEST_TIMEOUT,
  });
  const name = res.status === 200 ? (res.json('results.0.cafeName') || '') : '';
  if (!name.startsWith('(가짜 AI)')) {
    fail(`가짜 AI 서버가 붙어 있지 않습니다(픽봇 HTTP ${res.status}, 첫 결과 "${name}"). `
      + 'docs/MONITORING.md 의 "느린 AI 테스트" 대로 web 을 가짜 AI 서버로 다시 만든 뒤 실행하세요.');
  }
}

export function setup() {
  const data = prepare();
  if (TARGET === 'review' && !data.token) {
    fail('SLOW_TARGET=review 는 로그인 계정이 필요합니다. .env 의 K6_USER_EMAIL·K6_USER_PASSWORD 를 채우세요.');
  }
  assertStubAttached();
  console.log(`느린 요청: ${TARGET}, 단계 ${STEPS.join(' → ')}건, 열람 ${BROWSE_VUS}명`);
  return data;
}

// 열람: load.js 와 같은 비로그인 흐름. 화면 사이마다 구간 태그를 새로 붙입니다.
const think = () => {
  sleep(1 + Math.random() * 2);
  tagStep();
};

export function browse(data) {
  tagStep();
  browseAsGuest(data, think);
}

function askPickBot() {
  const res = http.post(
    `${BASE_URL}/api/pickbot/recommend`,
    JSON.stringify({ query: pick(PICKBOT_QUERIES) }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'pickbot' }, timeout: SLOW_REQUEST_TIMEOUT },
  );
  check(res, { 'pickbot 200': (r) => r.status === 200 });
}

/** 리뷰 작성 폼 제출. 성공하면 카페 상세로 302, 로그인이 풀렸으면 /login 으로 302 라 Location 까지 봅니다. */
function writeReview(data) {
  const res = http.post(
    `${BASE_URL}/reviews/new`,
    {
      cafeId: String(pick(data.cafeIds)),
      reviewContent: `${REVIEW_MARK} 느린 AI 테스트에서 쓴 리뷰입니다. 커피와 분위기가 좋아요.`,
    },
    {
      headers: { Authorization: `Bearer ${data.token}` },
      redirects: 0,
      responseCallback: http.expectedStatuses(302),
      tags: { name: 'review-write' },
      timeout: SLOW_REQUEST_TIMEOUT,
    },
  );
  check(res, {
    'review-write 302 → 카페 상세': (r) => r.status === 302 && /\/cafes\/\d+/.test(r.headers.Location || ''),
  });
}

export function slow(data) {
  tagStep();
  if (TARGET === 'review') {
    writeReview(data);
  } else {
    askPickBot();
  }
  sleep(1);
}
