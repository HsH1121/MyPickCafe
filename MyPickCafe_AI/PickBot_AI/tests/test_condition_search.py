"""
픽봇 조건별 검색 테스트 — LLM·DB·ChromaDB·Ollama 없이 실행된다.

조건 벡터와 리뷰 벡터를 손으로 정한 가짜 ChromaDB(코사인 거리를 실제로 계산)로 확인한다.
- 1차 LLM 이 준 조건 목록을 그대로 받고, 예전 형식(조건 문장 하나)과 1차 실패는 문장 하나를 조건 하나로 본다.
- 조건 임베딩은 한 번에(batch), ChromaDB 조회는 모든 조건을 한 번에, 저장된 리뷰 임베딩을 받아 재임베딩하지 않는다.
- 조건마다 최대 N개, 카페당 상한. 한 카페가 상위를 독차지해 모자란 조건만 상한이 찬 카페를 빼고 다시 묻는다.
- 후보 리뷰 전부를 모든 조건과 다시 비교한다(P1 으로 들어온 리뷰가 P2 도 충족).
- 순위: 리뷰 하나로 전부 > 여러 리뷰로 전부 > 일부, 같으면 유사도. 판정 LLM 에는 카페당 대표 리뷰 3개 이하.
- 판정 LLM 메시지에 요구사항 목록이 그대로 들어가고, 판정 결과의 요구사항도 그 목록이다.

사용법 (MyPickCafe_AI/ 에서)
  python PickBot_AI/tests/test_condition_search.py
"""

from __future__ import annotations
import asyncio
import math
import sys
import tempfile
from pathlib import Path
from types import SimpleNamespace

sys.path.insert(0, str(Path(__file__).parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

import pickbot_rag
import query_parser
from config import Settings

P1, P2, P3 = [1.0, 0.0, 0.0], [0.0, 1.0, 0.0], [0.0, 0.0, 1.0]
COND_VEC = {"와이파이가 빠름": P1, "주차가 편함": P2, "디저트가 맛있음": P3}


def _cos(a, b) -> float:
    return sum(x * y for x, y in zip(a, b)) / (math.sqrt(sum(x * x for x in a)) * math.sqrt(sum(y * y for y in b)))


class FakeCollection:
    """reviews: [(review id, cafe_id, 본문, 임베딩)]. query 는 where 를 지키고 코사인 거리 순으로 돌려준다."""

    def __init__(self, reviews) -> None:
        self.reviews = reviews
        self.calls: list[dict] = []

    def count(self) -> int:
        return len(self.reviews)

    def query(self, query_embeddings, n_results, where=None, include=None):
        self.calls.append({"n": len(query_embeddings), "n_results": n_results, "where": where, "include": include})
        rows = self.reviews
        for c in (where.get("$and", [where]) if where else []):
            rule = c["cafe_id"]
            if "$in" in rule:
                rows = [r for r in rows if r[1] in rule["$in"]]
            if "$nin" in rule:
                rows = [r for r in rows if r[1] not in rule["$nin"]]
        out = {"ids": [], "metadatas": [], "distances": [], "embeddings": []}
        for q in query_embeddings:
            ranked = sorted(rows, key=lambda r: 1 - _cos(q, r[3]))[:n_results]
            out["ids"].append([r[0] for r in ranked])
            out["metadatas"].append([{"cafe_id": r[1], "cafe_name": f"카페{r[1]}", "address": f"서울시 용산구 {r[1]}",
                                      "review": r[2]} for r in ranked])
            out["distances"].append([1 - _cos(q, r[3]) for r in ranked])
            out["embeddings"].append([r[3] for r in ranked])
        return out


class FakeEmbedding:
    def __init__(self) -> None:
        self.calls: list[list[str]] = []

    def __call__(self, texts):
        self.calls.append(list(texts))
        return [COND_VEC.get(t, P1) for t in texts]


def make_rag(reviews, **overrides) -> pickbot_rag.CafeRAG:
    rag = object.__new__(pickbot_rag.CafeRAG)
    rag.settings = Settings(llm_reasoning_effort="", **overrides)
    rag._col = FakeCollection(reviews)
    rag._emb_fn = FakeEmbedding()
    return rag


# 카페 1: 리뷰 하나로 와이파이+주차 / 카페 2: 리뷰 둘로 와이파이, 주차 / 카페 3: 와이파이만(더 높은 유사도) / 카페 4: 주차만
REVIEWS = [
    ("r1", "1", "와이파이 빠르고 주차도 편해요", [1.0, 1.0, 0.0]),
    ("r2", "2", "와이파이가 정말 빨라요", [1.0, 0.2, 0.0]),
    ("r3", "2", "건물 뒤 주차장 넓어요", [0.2, 1.0, 0.0]),
    ("r4", "3", "와이파이 최고", [1.0, 0.0, 0.0]),
    ("r5", "4", "주차 편함", [0.0, 1.0, 0.1]),
]


def main() -> int:
    failures = 0

    def expect(name: str, ok: bool, detail: object = "") -> None:
        nonlocal failures
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] {name}" + ("" if ok else f"\n       {detail}"))

    # 1. 1차 LLM — 조건 목록
    async def fake_llm(**kw):
        return answer

    with tempfile.TemporaryDirectory() as tmp:
        settings = Settings(llm_reasoning_effort="", region_candidates_path=str(Path(tmp) / "c.json"))
        query_parser.call_llm = fake_llm
        answer = {"regions": ["용산"], "exclude_regions": [], "conditions": ["와이파이가 빠름", "주차가 편함"]}
        p = asyncio.run(query_parser.parse_query("용산 와이파이 빠르고 주차 편한 카페", ["용산구"], settings))
        expect("1차 LLM 조건 목록을 그대로 받음", p.regions == ["용산구"]
               and p.condition_list() == ["와이파이가 빠름", "주차가 편함"], p)
        answer = {"regions": [], "exclude_regions": [], "purpose": "조용한 카페"}
        p = asyncio.run(query_parser.parse_query("조용한 카페", [], settings))
        expect("예전 형식(조건 문장)은 문장 하나를 조건 하나로", p.condition_list() == ["조용한 카페"], p)
        answer = {"regions": ["성수"], "exclude_regions": [], "conditions": []}
        p = asyncio.run(query_parser.parse_query("성수 카페", ["성수동"], settings))
        expect("조건이 없으면 빈 목록(지역만 → 긍정 리뷰 순위 경로)", p.condition_list() == [], p)
    expect("1차 실패 결과(purpose=질문 전체)도 조건 하나", query_parser.ParsedQuery(purpose="망원동 커피").condition_list()
           == ["망원동 커피"])

    # 2. 조건별 검색 → 커버리지 순위
    rag = make_rag(REVIEWS)
    conds = ["와이파이가 빠름", "주차가 편함"]
    top = asyncio.run(rag._rag_cafes(conds, None))
    order = [c["cafe_id"] for c in top]
    expect("조건 임베딩은 한 번에(batch)", rag._emb_fn.calls == [conds], rag._emb_fn.calls)
    call = rag._col.calls[0] if rag._col.calls else {}
    expect("ChromaDB 조회는 모든 조건을 한 번에, 저장 임베딩 포함",
           len(rag._col.calls) == 1 and call["n"] == 2 and "embeddings" in call["include"], rag._col.calls)
    expect("순위: 리뷰 하나로 전부 > 여러 리뷰로 전부 > 일부(유사도 순)", order == [1, 2, 3, 4], order)
    by_id = {c["cafe_id"]: c for c in top}
    expect("커버리지·등급", [(by_id[i]["covered"], by_id[i]["tier"]) for i in (1, 2, 3, 4)]
           == [(2, 2), (2, 1), (1, 0), (1, 0)], {i: (c["covered"], c["tier"]) for i, c in by_id.items()})
    expect("조건별 근거 리뷰 보관", [e["review"] for e in by_id[2]["evidence"]]
           == ["와이파이가 정말 빨라요", "건물 뒤 주차장 넓어요"], by_id[2]["evidence"])
    expect("리뷰 하나가 두 조건 근거", by_id[1]["reviews"] == [
        {"text": "와이파이 빠르고 주차도 편해요", "conditions": conds}], by_id[1]["reviews"])

    # 3. 교차 비교 — 주차로만 검색돼 들어온 리뷰가 와이파이도 충족
    cross = [
        ("a", "1", "와이파이 빠름", [1.0, 0.0, 0.0]),
        ("b", "2", "주차 편하고 와이파이도 빨라요", [0.8, 1.0, 0.0]),
        ("c", "3", "와이파이 괜찮아요", [1.0, 0.05, 0.0]),
    ]
    rag = make_rag(cross, pickbot_reviews_per_condition=1)
    top = asyncio.run(rag._rag_cafes(conds, None))
    b = next((c for c in top if c["cafe_id"] == 2), None)
    expect("P2 로 들어온 리뷰도 P1 과 다시 비교해 2/2", b is not None and b["covered"] == 2 and top[0]["cafe_id"] == 2,
           [(c["cafe_id"], c["covered"]) for c in top])

    # 4. 조건마다 최대 N개, 카페당 상한 — 리뷰 많은 카페가 상위를 독차지해도 7곳 이상
    flood = [(f"x{i}", "9", f"와이파이 {i}", [1.0, 0.001 * i, 0.0]) for i in range(80)]
    flood += [(f"y{k}", str(k), f"와이파이 괜찮음 {k}", [1.0, 0.3, 0.0]) for k in range(1, 9)]
    rag = make_rag(flood)
    pool, per_cond, queries = rag._search_by_condition([P1], None)
    cafes = [pool[r]["meta"]["cafe_id"] for r in per_cond[0]]
    expect("카페당 3개 상한, 상한 찬 카페 빼고 보충 조회", cafes.count("9") == 3 and len(set(cafes)) == 9
           and len(cafes) == 3 + 8 and queries == 2, (cafes, queries))
    many = [(f"z{k}-{j}", str(k), f"와이파이 {k}-{j}", [1.0, 0.01 * k + 0.001 * j, 0.0])
            for k in range(1, 11) for j in range(5)]
    rag = make_rag(many)
    pool, per_cond, queries = rag._search_by_condition([P1], None)
    cafes = [pool[r]["meta"]["cafe_id"] for r in per_cond[0]]
    expect("조건별 21개, 카페당 3개 → 7곳, 조회 1번", len(cafes) == 21 and len(set(cafes)) == 7
           and max(cafes.count(c) for c in cafes) == 3 and queries == 1, (len(cafes), len(set(cafes)), queries))

    # 5. 지역 필터 유지
    rag = make_rag(REVIEWS)
    top = asyncio.run(rag._rag_cafes(conds, ["2", "4"]))
    expect("지역 후보 카페로만 검색", sorted(c["cafe_id"] for c in top) == [2, 4]
           and rag._col.calls[0]["where"] == {"cafe_id": {"$in": ["2", "4"]}}, rag._col.calls)

    # 6. 조건 1개 — 카페당 대표 리뷰를 3개까지 채움
    single = [(f"s{j}", "1", f"와이파이 좋아요 {j}", [1.0, 0.01 * j, 0.0]) for j in range(5)]
    rag = make_rag(single)
    top = asyncio.run(rag._rag_cafes(["와이파이가 빠름"], None))
    expect("조건 1개면 카페당 리뷰 3개(있는 만큼)", len(top) == 1 and len(top[0]["reviews"]) == 3, top)

    # 7. 판정 상위 카페 수
    rag = make_rag(many, pickbot_judge_max_cafes=5)
    top = asyncio.run(rag._rag_cafes(["와이파이가 빠름"], None))
    expect("판정 LLM 에는 상위 카페만", len(top) == 5, len(top))

    # 8. 판정 LLM 메시지·결과
    rag = make_rag(REVIEWS)
    top = asyncio.run(rag._rag_cafes(conds, None))
    msg = pickbot_rag.build_pick_user_message(conds, top)
    expect("요구사항 목록을 그대로 넘김", '요구사항 (이 목록 그대로 판정): ["와이파이가 빠름", "주차가 편함"]' in msg
           and "리뷰1 (검색 근거: 와이파이가 빠름, 주차가 편함): 와이파이 빠르고 주차도 편해요" in msg, msg)
    raw = {"cafes": [{"cafeId": 1, "matched": conds, "missing": [], "snippet": "둘 다 좋아요"},
                     {"cafeId": 2, "matched": ["와이파이가 빠름"], "missing": ["주차가 편함"], "snippet": "와이파이"}]}
    picks, stats = pickbot_rag.validate_llm_response_and_select_cafes(raw, top, conds)
    expect("판정 요구사항은 넘긴 목록, 모두 충족만 반환", stats["requirements"] == conds
           and [p["cafeId"] for p in picks] == [1], (stats, picks))
    # 실측(2026-10-06): 모두 충족한 카페의 missing 을 빈 배열 대신 생략(null)해 모두 충족 5곳이 1곳으로 줄었다
    raw = {"cafes": [{"cafeId": 2, "matched": conds, "missing": None, "snippet": "둘 다"},
                     {"cafeId": 1, "matched": conds, "snippet": "둘 다"},
                     {"cafeId": 3, "matched": ["와이파이가 빠름"], "snippet": "와이파이"}]}
    picks, stats = pickbot_rag.validate_llm_response_and_select_cafes(raw, top, conds)
    expect("missing 을 생략하면 요구사항 목록에서 matched 를 빼서 계산", [p["cafeId"] for p in picks] == [1, 2]
           and stats["verdicts"][3]["missing"] == ["주차가 편함"], (picks, stats))
    expect("판정 프롬프트는 다시 나누지 말라고 함", "다시 나누거나 합치거나 바꾸지 마세요" in pickbot_rag._SYSTEM_PROMPT
           and "requirements" not in pickbot_rag._SYSTEM_PROMPT)

    # 9. LLM 호출 수 — 조건이 3개여도 1차 1번 + 판정 1번
    calls = []

    async def counting_llm(**kw):
        calls.append(kw["system_prompt"])
        if kw["system_prompt"] is query_parser.SYSTEM_PROMPT:
            return {"regions": [], "exclude_regions": [], "conditions": list(COND_VEC)}
        return {"cafes": []}

    query_parser.call_llm = counting_llm
    pickbot_rag.call_llm = counting_llm
    rag = make_rag(REVIEWS + [("r6", "5", "디저트 맛있어요", [0.0, 0.0, 1.0])])
    rag._directory = [{"cafe_id": i, "cafe_name": f"카페{i}", "address": f"서울시 용산구 {i}", "review_count": 1,
                       "good_count": 1} for i in range(1, 6)]
    rag._directory_loaded_at = 1e18
    asyncio.run(rag.recommend("와이파이 빠르고 주차 편하고 디저트 맛있는 카페"))
    expect("조건 3개여도 LLM 호출 2번, 임베딩 1번, 조회 1번", len(calls) == 2 and len(rag._emb_fn.calls) == 1
           and len(rag._col.calls) == 1, (len(calls), rag._emb_fn.calls, len(rag._col.calls)))

    total = 20
    print(f"\n{total - failures}/{total} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
