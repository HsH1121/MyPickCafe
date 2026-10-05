"""
픽봇 질문 길이 제한(서버 60자) 검증 테스트 — 서버·LLM 없이 실행된다.

- 글자 수는 프론트엔드(cafes/list.mustache 의 Array.from(value).length)와 같은
  "유니코드 코드 포인트" 기준이다. 공백·특수문자·이모지도 각각 1자로 센다.
- 프론트 입력 제한은 50자, 서버 제한은 여유를 둔 60자다.
  프론트 최대치(50자)는 반드시 통과하고, 60자까지 통과, 61자부터는 거절돼야 한다.

사용법 (MyPickCafe_AI/ 에서)
  python PickBot_AI/test_query_length.py
"""

from __future__ import annotations
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
sys.stdout.reconfigure(encoding="utf-8")

from fastapi import FastAPI
from fastapi.testclient import TestClient
from pydantic import ValidationError

from schemas import PickBotRequest

# (설명, 질문, 기대 글자 수, 통과해야 하는가)
CASES: list[tuple[str, str, int, bool]] = [
    ("프론트 최대 50자",          "가" * 50,                          50, True),
    ("한글 60자",                "가" * 60,                          60, True),
    ("한글 61자",                "가" * 61,                          61, False),
    ("공백 포함 60자",            "조용한 카페 " * 8 + "?!?!",         60, True),   # 7자 × 8 + 4
    ("공백 포함 61자",            "조용한 카페 " * 8 + "?!?!!",        61, False),
    ("특수문자 60자",             "!@#$%^&*()" * 6,                   60, True),
    ("앞뒤 공백 포함 61자",        " " + "가" * 59 + " ",               61, False),
    ("이모지 60자(코드 포인트)",   "☕" * 20 + "😀" * 20 + "가" * 20,   60, True),   # 😀 는 UTF-16 으로 2칸이지만 1자
    ("이모지 61자",               "😀" * 61,                          61, False),
]


def build_app() -> FastAPI:
    """app.py 의 /pickbot/recommend 와 같은 요청 모델을 쓰는 최소 앱 (RAG·DB 없이 검증만 확인)."""
    app = FastAPI()

    @app.post("/pickbot/recommend")
    async def recommend(request: PickBotRequest) -> dict:
        return {"query": request.query}

    return app


def main() -> int:
    client = TestClient(build_app())
    failures = 0

    for name, query, expected_len, should_pass in CASES:
        assert len(query) == expected_len, f"테스트 데이터 길이 오류: {name} ({len(query)})"

        try:
            PickBotRequest(query=query)
            model_ok = True
        except ValidationError:
            model_ok = False

        status = client.post("/pickbot/recommend", json={"query": query}).status_code
        http_ok = status == 200

        ok = model_ok == should_pass and http_ok == should_pass and status in (200, 422)
        failures += not ok
        expect = "통과" if should_pass else "거절(422)"
        print(f"[{'OK ' if ok else 'FAIL'}] {name:<22} 기대={expect:<8} 모델={'통과' if model_ok else '거절'} HTTP={status}")

    print(f"\n{len(CASES) - failures}/{len(CASES)} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
