"""
픽봇 질문 길이 제한(30자) 검증 테스트 — 서버·LLM 없이 실행된다.

- 글자 수는 프론트엔드(cafes/list.mustache 의 Array.from(value).length)와 같은
  "유니코드 코드 포인트" 기준이다. 공백·특수문자·이모지도 각각 1자로 센다.
- 프론트에서 30자로 보낸 질문이 여기서 거절되면 안 되고, 31자부터는 거절돼야 한다.

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

LIMIT = 30

# (설명, 질문, 통과해야 하는가)
CASES: list[tuple[str, str, bool]] = [
    ("한글 30자",               "가" * 30,                         True),
    ("한글 31자",               "가" * 31,                         False),
    ("공백 포함 30자",           "조용한 카페 " * 4 + "?!",          True),   # 7자 × 4 + 2
    ("공백 포함 31자",           "조용한 카페 " * 4 + "?!!",         False),
    ("특수문자 30자",            "!@#$%^&*()" * 3,                  True),
    ("앞뒤 공백 포함 31자",       " " + "가" * 29 + " ",              False),
    ("이모지 30자(코드 포인트)",  "☕" * 10 + "😀" * 10 + "가" * 10,  True),   # 😀 는 UTF-16 으로 2칸이지만 1자
    ("이모지 31자",              "😀" * 31,                         False),
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

    for name, query, should_pass in CASES:
        assert len(query) == (LIMIT if should_pass else LIMIT + 1), f"테스트 데이터 길이 오류: {name} ({len(query)})"

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
