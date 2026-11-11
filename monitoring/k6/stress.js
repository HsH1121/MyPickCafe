// 스트레스 테스트 — 쉬지 않고 요청하며 사람을 계단식으로 늘려, 어디서부터 버거워하는지(한계) 찾습니다.
//   docker compose run --rm k6 run -o experimental-prometheus-rw /scripts/stress.js
// 약 7분 걸립니다. 기준을 어기는(종료 코드 99) 것이 정상일 수 있습니다. 어긴 단계가 이 서버의 한계입니다.
//
// 계단은 이 앱의 대기열 두 개에 맞췄습니다. 각 단계를 1분씩 유지해 Grafana 에서 단계별로 읽습니다.
//   20명  : DB 커넥션 풀 크기(prod 의 hikari maximum-pool-size=20). 넘으면 요청이 커넥션을 기다리기 시작합니다.
//   100명 : 그 사이
//   200명 : Tomcat 기본 스레드 수(200). 넘으면 요청이 스레드를 기다리며 줄을 섭니다.
//   300명 : 스레드 수를 넘긴 상태
// "k6 부하 테스트" 대시보드에서 VU 가 늘어도 초당 요청이 더 늘지 않는 지점, DB 커넥션 대기(pending)와
// Tomcat busy 스레드가 차는 순서를 같이 보세요.
import { sleep } from 'k6';
import {
  HAS_MEMBER, SUMMARY_TREND_STATS, SYSTEM_TAGS,
  browseAsGuest, browseAsMember, login, prepare, scaleStages,
} from './lib/mypickcafe.js';

const MEMBER_RATIO = HAS_MEMBER ? 0.2 : 0;

const STAGES = [
  { duration: '30s', target: 20 },
  { duration: '1m', target: 20 },
  { duration: '30s', target: 100 },
  { duration: '1m', target: 100 },
  { duration: '30s', target: 200 },
  { duration: '1m', target: 200 },
  { duration: '30s', target: 300 },
  { duration: '1m', target: 300 },
  { duration: '30s', target: 0 },   // 부하를 빼면 회복되는지
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

export const options = {
  scenarios,
  // 한계 판정 기준: 사용자가 느리다고 느끼기 시작하는 1초, 실패 1%
  thresholds: {
    'http_req_duration{expected_response:true}': ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
  },
  systemTags: SYSTEM_TAGS,
  summaryTrendStats: SUMMARY_TREND_STATS,
};

export function setup() {
  return prepare();
}

// 쉬지 않고 요청합니다. 같은 VU 수로 load 보다 훨씬 큰 부하를 만듭니다.
const think = () => {};

export function guest(data) {
  browseAsGuest(data, think);
}

let token = null;
export function member(data) {
  token = token || login();
  if (!token) {
    sleep(1);
    return;
  }
  browseAsMember(data, token, think);
}
