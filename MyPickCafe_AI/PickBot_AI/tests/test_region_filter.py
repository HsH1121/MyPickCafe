"""
픽봇 지역 필터 분기 테스트 — LLM·DB·ChromaDB·Ollama 없이 실행된다.

질문 분해 결과(ParsedQuery)를 직접 넣고, recommend() 가 어떤 후보로 검색하는지와 안내(notice)를 확인한다.
- 허용 목록에 없는 지역(unmatched_regions)만 있으면 지역 필터 없이 전체 카페에서 검색하고 안내도 없다.
  모든 카페가 서울에 있어 "서울"을 지역으로 잘못 뽑거나, "제주 말차"의 제주를 지역으로 착각해도
  추천이 막히지 않게 하기 위함이다.
- 허용 지역이 하나라도 있으면 그 지역으로만 거른다. 허용 목록 밖의 지역은 무시한다.
- 허용 지역으로 걸렀는데 남는 카페가 없을 때만 REGION_NOT_FOUND 를 안내한다.
- 서울 밖 지명(outside_regions)을 말하면 그 지역은 거르지 않고 찾은 결과와 함께 OUTSIDE_SEOUL 을 안내한다.

사용법 (MyPickCafe_AI/ 에서)
  python PickBot_AI/tests/test_region_filter.py
"""

from __future__ import annotations
import asyncio
import sys
from types import SimpleNamespace
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

import pickbot_rag
from query_parser import ParsedQuery

DIRECTORY = [
    {"cafe_id": 1, "cafe_name": "A", "address": "서울시 마포구 연남동 1-1", "review_count": 3, "good_count": 2},
    {"cafe_id": 2, "cafe_name": "B", "address": "서울시 마포구 망원동 2-2", "review_count": 2, "good_count": 1},
    {"cafe_id": 3, "cafe_name": "C", "address": "서울시 성동구 성수동 3-3", "review_count": 5, "good_count": 4},
]
ALL = None  # 지역 필터 없이 전체 검색

# (설명, 질문 분해 결과, 기대: 벡터 검색 후보 cafe_id(ALL=필터 없음) 또는 지역만일 때 순위 후보, 기대 안내)
CASES = [
    ("허용 목록 밖 지역만 (제주도 오션뷰)",
     ParsedQuery(unmatched_regions=["제주도"], purpose="오션뷰 카페"),             ("rag", ALL),          None),
    ("서울을 지역으로 뽑음 (서울 아무 데나)",
     ParsedQuery(unmatched_regions=["서울"], purpose="대형 카페"),                 ("rag", ALL),          None),
    ("허용 목록 밖 지역만, 조건 없음 (판교 카페)",
     ParsedQuery(unmatched_regions=["판교"]),                                     ("rank", [1, 2, 3]),   None),
    ("허용 지역 + 목록 밖 지역 (판교나 성수)",
     ParsedQuery(regions=["성수동"], unmatched_regions=["판교"], purpose="작업"),   ("rag", ["3"]),        None),
    ("허용 지역만 (마포구)",
     ParsedQuery(regions=["마포구"], purpose="주차"),                              ("rag", ["1", "2"]),   None),
    ("제외 지역 (마포구 말고)",
     ParsedQuery(exclude_regions=["마포구"], purpose="공부"),                      ("rag", ["3"]),        None),
    ("제외로 전부 빠짐",
     ParsedQuery(exclude_regions=["마포구", "성동구"], purpose="공부"),             ("none", None),        pickbot_rag.NOTICE_REGION_NOT_FOUND),
    ("서울 밖 지명 (판교 디저트)",
     ParsedQuery(unmatched_regions=["판교"], outside_regions=["판교"], purpose="디저트"),
     ("rag", ALL), pickbot_rag.NOTICE_OUTSIDE_SEOUL),
    ("서울 밖 지명, 조건 없음 (제주도 카페)",
     ParsedQuery(unmatched_regions=["제주도"], outside_regions=["제주도"]),
     ("rank", [1, 2, 3]), pickbot_rag.NOTICE_OUTSIDE_SEOUL),
    ("서울 밖 + 서울 지역 (판교나 성수)",
     ParsedQuery(regions=["성수동"], unmatched_regions=["판교"], outside_regions=["판교"], purpose="작업"),
     ("rag", ["3"]), pickbot_rag.NOTICE_OUTSIDE_SEOUL),
]


class _FakeCollection:
    def count(self) -> int:
        return 1


def make_rag(parsed: ParsedQuery, calls: list) -> pickbot_rag.CafeRAG:
    """ChromaDB·임베딩 없이 recommend() 의 분기만 확인하도록 외부 호출을 기록용 가짜로 바꾼다."""
    rag = object.__new__(pickbot_rag.CafeRAG)
    rag.settings = SimpleNamespace(pickbot_trace=False)
    rag._col = _FakeCollection()

    async def directory():
        return DIRECTORY

    async def rag_cafes(text, candidate_ids, top_n, trace=None):
        calls.append(("rag", candidate_ids))
        return [{"cafe_id": 1}]

    async def rank(candidates, top_n, trace=None):
        calls.append(("rank", [c["cafe_id"] for c in candidates]))
        return []

    async def pick(purpose, top_cafes, trace=None):
        return []

    rag._cafe_directory = directory
    rag._rag_cafes = rag_cafes
    rag._rank_with_only_region = rank
    rag._pick_with_llm = pick

    async def parse(query, allowed, settings, trace=None):
        return parsed

    pickbot_rag.parse_query = parse
    return rag


def main() -> int:
    failures = 0
    for name, parsed, (kind, expected_ids), expected_notice in CASES:
        calls: list = []
        result = asyncio.run(make_rag(parsed, calls).recommend("질문"))
        got_kind, got_ids = calls[0] if calls else ("none", None)
        if got_ids is not None:
            got_ids = sorted(got_ids)
        ok = (got_kind, got_ids) == (kind, expected_ids) and result["notice"] == expected_notice
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] {name}")
        if not ok:
            print(f"       기대: {kind} {expected_ids} notice={expected_notice}")
            print(f"       실제: {got_kind} {got_ids} notice={result['notice']}")

    print(f"\n{len(CASES) - failures}/{len(CASES)} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
