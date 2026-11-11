// 스모크 테스트 — VU 1명이 모든 화면을 한 번씩 열어 전부 200 을 주는지 확인합니다. 큰 테스트 전에 항상 먼저 돌립니다.
//   docker compose run --rm k6 run /scripts/smoke.js
//
// 하나라도 실패하면 종료 코드 99 입니다. 이 상태로 load/stress 를 돌리면 에러만 재게 됩니다.
import { sleep } from 'k6';
import {
  HAS_MEMBER, SUMMARY_TREND_STATS, SYSTEM_TAGS,
  login, prepare, visitEveryPage,
} from './lib/mypickcafe.js';

export const options = {
  vus: 1,
  duration: '20s',
  systemTags: SYSTEM_TAGS,
  summaryTrendStats: SUMMARY_TREND_STATS,
  thresholds: {
    checks: ['rate==1'],          // 모든 응답이 200
    http_req_failed: ['rate==0'],
  },
};

export function setup() {
  return prepare();
}

let token = null;

export default function (data) {
  if (HAS_MEMBER) token = token || login();
  visitEveryPage(data, token, () => sleep(1));
}
