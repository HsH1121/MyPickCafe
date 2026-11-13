"""가짜 AI 서버 — 느린 AI 부하 테스트(monitoring/k6/slow-ai.js)에서 실제 AI 서버(MyPickCafe_AI) 대신 붙입니다.

LLM·임베딩을 부르지 않고, 정해 둔 시간만큼 기다렸다가 형식만 맞는 응답을 줍니다.
AI 응답을 기다리는 동안 Spring 이 스레드·DB 커넥션을 어떻게 쓰는지를 LLM 비용 없이 재려는 것입니다.

흉내 내는 엔드포인트 (Spring 의 PickBotClient·PythonTagClient 가 부르는 것)
  POST /pickbot/recommend    5~15초 기다린 뒤, 요청의 approvedCafeIds 앞쪽 3곳을 추천 결과로 줍니다.
                             (결과가 있어야 Spring 이 응답 뒤에 대표 사진 조회까지 실제와 같게 합니다.)
  POST /review/analyze       2~4초 기다린 뒤 빈 태그·감성 없음을 줍니다.
                             (태그를 주면 테스트 리뷰가 카페 대표 태그를 바꿉니다.)
  POST /pickbot/index-one    바로 200. 리뷰 저장 뒤 Spring 이 결과를 기다리지 않고 부릅니다.
  POST /pickbot/delete-one   바로 200.
  GET  /health               바로 200.

환경 변수
  PORT                  기본 8000
  PICKBOT_DELAY_MIN_MS  기본 5000   픽봇 한 번(질문 분해 + 검색 + 판정 LLM)에 걸리는 시간.
  PICKBOT_DELAY_MAX_MS  기본 15000  요청마다 최소~최대 사이에서 고르게(균등 분포) 뽑습니다.
  TAG_DELAY_MIN_MS      기본 2000   리뷰 태그 분석(LLM 한 번)에 걸리는 시간. 뽑는 방법은 위와 같습니다.
  TAG_DELAY_MAX_MS      기본 4000
  최소와 최대를 같게 주면 매번 같은 시간을 기다립니다.

10초마다 지금 기다리는 요청 수(in-flight)를 출력합니다. k6 의 VU 수가 실제로 몇 건의 동시 AI 호출이 됐는지 확인합니다.

표준 라이브러리만 씁니다.
  로컬:   python monitoring/ai-stub/ai_stub.py           (PORT 등은 환경 변수로)
  Docker: monitoring/ai-stub/docker-compose.ai-stub.yml  (docs/MONITORING.md 의 "느린 AI 테스트")
"""

import json
import os
import random
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("PORT", "8000"))


def delay_range(name, default_min, default_max):
    """환경 변수 {name}_MIN_MS·{name}_MAX_MS 를 읽어 (최소, 최대) ms 를 돌려줍니다."""
    low = int(os.environ.get(f"{name}_MIN_MS", default_min))
    high = int(os.environ.get(f"{name}_MAX_MS", default_max))
    if low < 0 or high < low:
        raise SystemExit(f"{name}_MIN_MS({low}) 는 0 이상이고 {name}_MAX_MS({high}) 이하여야 합니다.")
    return low, high


PICKBOT_DELAY = delay_range("PICKBOT_DELAY", 5000, 15000)
TAG_DELAY = delay_range("TAG_DELAY", 2000, 4000)

EMPTY_TAGS = {"sentiment": None, "FACILITY": [], "MENU": [], "PURPOSE": [], "MOOD": []}


class Stats:
    """엔드포인트별로 지금 기다리는 요청 수, 최대치, 누적 건수."""

    def __init__(self):
        self._lock = threading.Lock()
        self.in_flight = {}
        self.peak = {}
        self.total = {}

    def enter(self, key):
        with self._lock:
            self.in_flight[key] = self.in_flight.get(key, 0) + 1
            self.peak[key] = max(self.peak.get(key, 0), self.in_flight[key])
            self.total[key] = self.total.get(key, 0) + 1

    def leave(self, key):
        with self._lock:
            self.in_flight[key] -= 1

    def line(self):
        with self._lock:
            if not self.total:
                return None
            return "  ".join(
                f"{k} 대기 {self.in_flight[k]} (최대 {self.peak[k]}, 누적 {self.total[k]})" for k in sorted(self.total)
            )


stats = Stats()


def wait(key, delay_range_ms):
    """(최소, 최대) ms 사이에서 고르게 뽑은 시간만큼 기다립니다. 그동안 key 의 대기 수에 잡힙니다."""
    stats.enter(key)
    try:
        time.sleep(random.uniform(*delay_range_ms) / 1000)
    finally:
        stats.leave(key)


class Handler(BaseHTTPRequestHandler):
    # keep-alive. Spring(WebClient)은 연결을 재사용하므로 요청마다 끊지 않습니다.
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        if self.path == "/health":
            self._send(200, {"status": "ok"})
        else:
            self._send(404, {"detail": "Not Found"})

    def do_POST(self):
        body = self._read_json()
        if self.path == "/pickbot/recommend":
            wait("pickbot", PICKBOT_DELAY)
            cafe_ids = body.get("approvedCafeIds") or []
            results = [
                {
                    "cafeId": cafe_id,
                    "cafeName": f"(가짜 AI) 카페 {cafe_id}",
                    "address": "",
                    "snippet": "가짜 AI 서버의 추천 결과입니다.",
                    "score": round(0.9 - i * 0.1, 2),
                }
                for i, cafe_id in enumerate(cafe_ids[:3])
            ]
            self._send(200, {"results": results, "notice": None})
        elif self.path == "/review/analyze":
            wait("tag", TAG_DELAY)
            self._send(200, EMPTY_TAGS)
        elif self.path in ("/pickbot/index-one", "/pickbot/delete-one"):
            self._send(200, {"status": "ok"})
        else:
            self._send(404, {"detail": "Not Found"})

    def _read_json(self):
        length = int(self.headers.get("Content-Length") or 0)
        if length == 0:
            return {}
        try:
            return json.loads(self.rfile.read(length))
        except ValueError:
            return {}

    def _send(self, status, payload):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, format, *args):
        # 요청마다 한 줄씩 찍으면 부하 중에 로그가 넘칩니다. 아래 report() 가 10초마다 요약합니다.
        pass


class Server(ThreadingHTTPServer):
    # listen 대기열. 기본값(5)은 연결이 한꺼번에 몰릴 때 거절될 수 있어 늘립니다(생성자에서 쓰이므로 클래스에 둡니다).
    request_queue_size = 128
    # Windows 의 SO_REUSEADDR 는 이미 쓰는 포트에도 조용히 같이 붙습니다. 로컬 AI 서버(8000)가 떠 있으면
    # 요청이 두 서버로 나뉘므로, Windows 에서는 끄고 포트가 겹치면 바로 실패하게 합니다.
    allow_reuse_address = os.name != "nt"


def report():
    last = None
    while True:
        time.sleep(10)
        line = stats.line()
        if line and line != last:
            print(time.strftime("%H:%M:%S"), line, flush=True)
            last = line


if __name__ == "__main__":
    threading.Thread(target=report, daemon=True).start()
    server = Server(("0.0.0.0", PORT), Handler)
    print(
        f"가짜 AI 서버 :{PORT}  픽봇 {PICKBOT_DELAY[0]}~{PICKBOT_DELAY[1]}ms, 태그 {TAG_DELAY[0]}~{TAG_DELAY[1]}ms",
        flush=True,
    )
    server.serve_forever()
