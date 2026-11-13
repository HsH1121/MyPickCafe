// 부하 테스트 — 평소 피크 트래픽에서 목표(SLO)를 지키는지 봅니다.
//   docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/load.js
//   (사람 수만 바꿀 때) docker compose run --rm -e PEAK_VUS=100 k6 run -o experimental-prometheus-rw /scripts/load.js
//
// 상황: 동시 사용자 50명이 화면을 1~3초씩 읽으며 둘러봅니다. 초당 요청 약 55건입니다(화면이 부르는 API 포함, 계산값).
//   - 개인이 운영하는 지역 카페 탐색 서비스(t3.large 한 대)의 피크를 넉넉히 잡은 값입니다.
//   - 로그인 계정(K6_USER_EMAIL)이 있으면 20%는 로그인 사용자(추천·알림·마이페이지)로 돕니다.
//
// 목표(SLO)는 테스트 전에 정한 값입니다. 결과를 보고 고치지 않습니다.
//   - 실패율 1% 미만, 응답 p95 500ms·p99 1.5초 미만, 로그인 p95 1초 미만(BCrypt 비용 포함)
// 하나라도 어기면 종료 코드 99 입니다.
import { sleep } from 'k6';
import {
  HAS_MEMBER, SUMMARY_TREND_STATS, SYSTEM_TAGS,
  browseAsGuest, browseAsMember, login, prepare, scaleStages,
} from './lib/mypickcafe.js';

const PEAK_VUS = Number(__ENV.PEAK_VUS || 50);
const MEMBER_RATIO = HAS_MEMBER ? 0.2 : 0;

const STAGES = [
  { duration: '1m', target: PEAK_VUS }, // 1분 동안 천천히 늘리기
  { duration: '3m', target: PEAK_VUS }, // 3분 유지 — 이 구간의 숫자가 판정 대상
  { duration: '30s', target: 0 },       // 줄이기
];

const scenarios = {
  guest: {
    executor: 'ramping-vus',
    exec: 'guest',
    startVUs: 0,
    stages: scaleStages(STAGES, 1 - MEMBER_RATIO),
    gracefulRampDown: '10s',
  },
};
if (HAS_MEMBER) {
  scenarios.member = {
    executor: 'ramping-vus',
    exec: 'member',
    startVUs: 0,
    stages: scaleStages(STAGES, MEMBER_RATIO),
    gracefulRampDown: '10s',
  };
}

const thresholds = {
  http_req_failed: ['rate<0.01'],
  'http_req_duration{expected_response:true}': ['p(95)<500', 'p(99)<1500'],
  checks: ['rate>0.99'],
};
if (HAS_MEMBER) thresholds['http_req_duration{name:login}'] = ['p(95)<1000'];

export const options = {
  scenarios,
  thresholds,
  systemTags: SYSTEM_TAGS,
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  return prepare();
}

// 화면을 읽는 시간 1~3초
const think = () => sleep(1 + Math.random() * 2);

export function guest(data) {
  browseAsGuest(data, think);
}

// VU 마다 처음 한 번만 로그인합니다(같은 사용자가 계속 둘러보는 상황).
let token = null;
export function member(data) {
  token = token || login();
  if (!token) {
    sleep(1);
    return;
  }
  browseAsMember(data, token, think);
}
