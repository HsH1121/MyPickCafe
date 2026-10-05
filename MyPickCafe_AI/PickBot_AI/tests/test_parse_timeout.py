"""
픽봇 질문 분해(LLM 1차 호출) 타임아웃 테스트 — LLM·DB 없이 실행된다.

- 질문 분해는 공용 llm_timeout(60초)이 아니라 llm_parse_timeout(기본 5초)으로 호출해야 한다.
- 타임아웃으로 끝내 실패하면 질문 전체를 조건으로 보고 지역 필터 없이 진행해야 한다.

사용법 (MyPickCafe_AI/ 에서)
  python PickBot_AI/tests/test_parse_timeout.py
"""

from __future__ import annotations
import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

import httpx

import query_parser
from config import Settings


def main() -> int:
    settings = Settings(llm_timeout=60, llm_parse_timeout=5)
    timeouts: list[int] = []

    async def fake_call_llm(**kwargs):
        timeouts.append(kwargs["timeout"])
        raise httpx.ReadTimeout("timed out")

    query_parser.call_llm = fake_call_llm
    parsed = asyncio.run(query_parser.parse_query("망원동 커피 맛집", ["마포구"], settings))

    checks = [
        ("질문 분해 타임아웃 = llm_parse_timeout(5초)", timeouts == [5], f"전달된 타임아웃 {timeouts}"),
        ("타임아웃이면 지역 없이 질문 전체를 조건으로", parsed == query_parser.ParsedQuery(purpose="망원동 커피 맛집"),
         f"결과 {parsed}"),
    ]
    failures = 0
    for name, ok, detail in checks:
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] {name}" + ("" if ok else f"\n       {detail}"))

    print(f"\n{len(checks) - failures}/{len(checks)} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
