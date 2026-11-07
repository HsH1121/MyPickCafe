"""
픽봇 승인 카페 사전 필터 테스트 — LLM·DB·ChromaDB·Ollama 없이 실행된다.

Spring 이 요청에 승인된 카페 ID 목록(approvedCafeIds)을 담아 보내면, 픽봇은 그 카페만 대상으로
지역 필터·벡터 검색(ChromaDB where)·순위를 계산한다. 추천 뒤에 거르지 않고 검색 전에 거른다.
- 카페 목록 캐시에 남은 승인 취소 카페는 검색 대상에서 빠진다.
- 승인 목록에 있는데 캐시에 없는 카페(캐시 이후 새로 승인)가 있으면 캐시를 바로 다시 읽는다.
- 조건 없는 순위(긍정 리뷰 수)도 승인 카페만 대상으로 한다.
- 승인 카페가 하나도 없으면 질문 분해·검색 없이 빈 결과를 돌려준다.
- 요청에 approvedCafeIds 가 없으면 422 로 거절한다(빠뜨렸을 때 전체 인덱스로 검색하지 않게).

사용법 (MyPickCafe_AI/ 에서)
  python PickBot_AI/tests/test_approved_prefilter.py
"""

from __future__ import annotations
import asyncio
import sys
import time
from pathlib import Path
from types import SimpleNamespace

sys.path.insert(0, str(Path(__file__).parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

from pydantic import ValidationError

import pickbot_rag
from query_parser import ParsedQuery
from schemas import PickBotRequest


def cafe(cafe_id: int) -> dict:
    return {"cafe_id": cafe_id, "cafe_name": f"카페{cafe_id}", "address": f"서울시 마포구 연남동 {cafe_id}-1",
            "review_count": 2, "good_count": 1}


class _FakeCollection:
    def count(self) -> int:
        return 1


def make_rag(cached: list[dict], fresh: list[dict], parsed: ParsedQuery, calls: list) -> pickbot_rag.CafeRAG:
    """카페 목록 캐시(cached)와 DB 를 다시 읽었을 때의 목록(fresh)을 정하고, 검색·순위 호출을 기록한다."""
    rag = object.__new__(pickbot_rag.CafeRAG)
    rag.settings = SimpleNamespace(pickbot_trace=False)
    rag._col = _FakeCollection()
    rag._directory = cached
    rag._directory_loaded_at = time.monotonic()  # 캐시가 아직 유효한 상태

    def fetch(settings):
        calls.append(("db", None))
        return fresh

    async def rag_cafes(conditions, candidate_ids, trace=None):
        calls.append(("rag", sorted(candidate_ids)))
        return []

    async def rank(candidates, top_n, trace=None):
        calls.append(("rank", sorted(c["cafe_id"] for c in candidates)))
        return []

    async def parse(query, allowed, settings, trace=None):
        calls.append(("parse", None))
        return parsed

    pickbot_rag.fetch_cafe_directory = fetch
    pickbot_rag.parse_query = parse
    rag._rag_cafes = rag_cafes
    rag._rank_with_only_region = rank
    return rag


failures = 0


def expect(name: str, ok: bool, detail=None) -> None:
    global failures
    failures += not ok
    print(f"[{'OK ' if ok else 'FAIL'}] {name}" + ("" if ok else f"\n       실제: {detail}"))


def main() -> int:
    with_condition = ParsedQuery(purpose="조용한 카페")
    no_condition = ParsedQuery()

    calls: list = []
    asyncio.run(make_rag([cafe(1), cafe(2), cafe(3)], [], with_condition, calls).recommend("질문", [1, 2]))
    expect("캐시에 남은 승인 취소 카페(3)는 벡터 검색 대상에서 빠진다",
           ("rag", ["1", "2"]) in calls and ("db", None) not in calls, calls)

    calls = []
    asyncio.run(make_rag([cafe(1)], [cafe(1), cafe(4)], with_condition, calls).recommend("질문", [1, 4]))
    expect("캐시 이후 새로 승인된 카페(4)가 있으면 목록을 다시 읽어 검색 대상에 넣는다",
           ("db", None) in calls and ("rag", ["1", "4"]) in calls, calls)

    calls = []
    asyncio.run(make_rag([cafe(1), cafe(2), cafe(3)], [], no_condition, calls).recommend("질문", [2, 3]))
    expect("조건 없는 순위도 승인 카페만 대상으로 한다", ("rank", [2, 3]) in calls, calls)

    calls = []
    result = asyncio.run(make_rag([cafe(1)], [cafe(1)], with_condition, calls).recommend("질문", []))
    expect("승인 카페가 없으면 질문 분해·검색 없이 빈 결과",
           result == {"results": [], "notice": None} and not any(k in ("parse", "rag", "rank") for k, _ in calls),
           (result, calls))

    try:
        PickBotRequest(query="조용한 카페")
        rejected = False
    except ValidationError:
        rejected = True
    expect("approvedCafeIds 없는 요청은 거절한다", rejected)
    req = PickBotRequest(query="조용한 카페", approvedCafeIds=[1, 2])
    expect("approvedCafeIds 를 받는다", req.approvedCafeIds == [1, 2], req)

    total = 6
    print(f"\n{total - failures}/{total} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
