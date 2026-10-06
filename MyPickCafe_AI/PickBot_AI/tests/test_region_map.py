"""
픽봇 지역 맵·질문 분해 흐름 테스트 — LLM·DB 없이 실행된다.

- 지역 맵: 지역 표현(역·별칭·줄임말·"근처" 등)을 허용 지역(카페 주소의 구·동 이름)으로 바꾸는지
- 질문 분해: 맵에 있으면 LLM 1차 호출만, 맵에 없는 표현만 LLM 에 동·구를 묻고 후보 파일에 기록하는지
- 서울 밖 지명(맵의 outside, LLM 의 seoul=false)은 거르지 않고 후보에도 넣지 않고 outside_regions 로 알리는지
- 지역 확인 호출이 실패하거나 서울인지 모르면 거르지도, 기록하지도 않는지

허용 지역은 현재 더미 주소 데이터의 구·동 이름이다(역 이름 홍대입구·건대입구가 동 자리에 있다).

사용법 (MyPickCafe_AI/ 에서)
  python PickBot_AI/tests/test_region_map.py
"""

from __future__ import annotations
import asyncio
import json
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

import query_parser
from config import Settings
from query_parser import ParsedQuery

ALLOWED = {"강남구", "강동구", "건대입구", "광진구", "마포구", "불광동", "서대문구", "성동구", "성수동", "송파구",
           "신사동", "여의도동", "연남동", "연희동", "영등포구", "용산구", "은평구", "이태원동", "익선동", "잠실동",
           "종로구", "천호동", "홍대입구"}

# (지역 표현, 기대 결과 — None 이면 맵에 없음)
MAP_CASES = [
    ("연트럴파크", ["연남동"]),
    ("연트럴 파크", ["연남동"]),
    ("망원동", ["마포구"]),
    ("상수역", ["마포구"]),
    ("홍대", ["홍대입구", "연남동"]),
    ("홍대입구역 근처", ["홍대입구", "연남동"]),
    ("건대 쪽", ["건대입구"]),
    ("잠실역", ["잠실동"]),
    ("가로수길", ["신사동"]),
    ("강남역", ["강남구"]),
    ("강남", ["강남구"]),
    ("성수", ["성수동"]),
    ("천호역 앞", ["천호동"]),
    ("여의도", ["여의도동"]),
    ("이태원", ["이태원동"]),
    ("종로", ["종로구"]),
    ("강동구", ["강동구"]),
    ("신촌", ["서대문구"]),
    ("노원", []),          # 서울이지만 데이터에 카페가 없는 구
    ("판교", []),          # 서울 밖
    ("제주도", []),
    ("서울", []),
    ("서울역", ["용산구"]),  # 끝의 "역"을 떼면 "서울"이 되지만 따로 둔 항목
    ("인천 송도", []),     # 지명 여러 개를 한 표현으로 받은 경우 — 단어마다 찾는다
    ("마포구 연남동", ["마포구", "연남동"]),
    ("송도 어딘가모르는동네", None),
    ("어딘가모르는동네", None),
]


# (지역 표현, 서울 밖인가) — 맵의 outside 표시
OUTSIDE_CASES = [
    ("판교", True),
    ("제주도", True),
    ("인천 송도", True),
    ("분당 정자동", True),
    ("서울", False),       # 서울 전체 — 지역 조건이 아닐 뿐 서울 밖이 아니다
    ("서울특별시", False),
    ("노원", False),       # 서울이지만 데이터에 카페가 없는 구
    ("연트럴파크", False),
    ("마포구 연남동", False),
]


def check_map() -> int:
    failures = 0
    for expr, expected in MAP_CASES:
        got = query_parser.REGION_MAP.resolve(expr, ALLOWED)
        ok = got == expected
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] 맵 {expr!r} → {got}" + ("" if ok else f"  (기대 {expected})"))
    for expr, expected in OUTSIDE_CASES:
        got = query_parser.REGION_MAP.is_outside(expr)
        ok = got == expected
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] 서울 밖 {expr!r} → {got}" + ("" if ok else f"  (기대 {expected})"))
    return failures


class FakeLLM:
    """1차 호출(질문 분해)과 지역 확인 호출을 프롬프트로 구분해 정해진 답을 준다."""

    def __init__(self, parse: dict | Exception, resolve: dict | Exception | None = None) -> None:
        self.parse, self.resolve = parse, resolve
        self.calls: list[str] = []

    async def __call__(self, **kw):
        kind = "resolve" if kw["system_prompt"] is query_parser.RESOLVE_SYSTEM_PROMPT else "parse"
        self.calls.append(kind)
        answer = self.parse if kind == "parse" else self.resolve
        if isinstance(answer, Exception):
            raise answer
        return answer


def run(query: str, llm: FakeLLM, candidates: Path) -> ParsedQuery:
    query_parser.call_llm = llm
    settings = Settings(region_candidates_path=str(candidates))
    return asyncio.run(query_parser.parse_query(query, sorted(ALLOWED), settings))


FLOW_CHECKS = 0


def check_flow() -> int:
    failures = 0

    def expect(name: str, ok: bool, detail: str) -> None:
        nonlocal failures
        global FLOW_CHECKS
        FLOW_CHECKS += 1
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] {name}" + ("" if ok else f"\n       {detail}"))

    with tempfile.TemporaryDirectory() as tmp:
        cand = Path(tmp) / "candidates.json"

        # 1. 맵에 있는 표현만 → LLM 1차 호출 한 번, 후보 기록 없음
        llm = FakeLLM({"regions": ["연트럴파크"], "exclude_regions": [], "purpose": "테라스 카페"})
        p = run("연트럴파크 근처 테라스 카페", llm, cand)
        expect("맵에 있는 표현은 LLM 1차 호출만", llm.calls == ["parse"] and p.regions == ["연남동"]
               and not cand.exists(), f"calls={llm.calls} parsed={p} 후보파일={cand.exists()}")

        # 2. 포함 + 제외
        llm = FakeLLM({"regions": ["연희동"], "exclude_regions": ["홍대"], "purpose": "조용한 카페"})
        p = run("홍대는 싫고 연희동 쪽 조용한 카페", llm, cand)
        expect("제외 지역도 맵으로 변환", p.regions == ["연희동"] and p.exclude_regions == ["홍대입구", "연남동"],
               f"parsed={p}")

        # 3. 맵에 없는 표현 → 지역 확인 호출, 데이터에 있는 구로 필터, 후보 기록
        llm = FakeLLM({"regions": ["합정카페거리"], "exclude_regions": [], "purpose": "브런치"},
                      {"results": {"합정카페거리": {"seoul": True, "areas": ["합정동", "서교동"], "gu": ["마포구"]}}})
        p = run("합정카페거리 브런치", llm, cand)
        data = json.loads(cand.read_text(encoding="utf-8")) if cand.exists() else {}
        item = data.get("합정카페거리", {})
        expect("맵에 없는 표현은 LLM 에 묻고 구로 필터", llm.calls == ["parse", "resolve"] and p.regions == ["마포구"],
               f"calls={llm.calls} parsed={p}")
        expect("후보 파일에 표현·횟수·예시·LLM 답 기록",
               item.get("count") == 1 and item.get("examples") == ["합정카페거리 브런치"]
               and item.get("llm") == {"areas": ["합정동", "서교동"], "gu": ["마포구"]}, f"후보={item}")

        # 4. 같은 표현이 또 나오면 횟수 누적
        run("합정카페거리 브런치", FakeLLM(llm.parse, llm.resolve), cand)
        item = json.loads(cand.read_text(encoding="utf-8"))["합정카페거리"]
        expect("같은 표현은 횟수 누적", item["count"] == 2 and len(item["examples"]) == 1, f"후보={item}")

        # 5. LLM 이 "성수동1가"처럼 법정동으로 답해도 주소의 동 이름(성수동)으로 맞춘다
        llm = FakeLLM({"regions": ["성수연방"], "exclude_regions": [], "purpose": "카페"},
                      {"results": {"성수연방": {"seoul": True, "areas": ["성수동2가"], "gu": ["성동구"]}}})
        p = run("성수연방 카페", llm, cand)
        expect("법정동 N가 → 동 이름", p.regions == ["성수동"], f"parsed={p}")

        def recorded(expr: str) -> bool:
            return expr in json.loads(cand.read_text(encoding="utf-8"))

        # 6. LLM 이 서울 밖이라고 답하면 거르지 않고, 기록하지 않고, 서울 밖으로 알린다
        llm = FakeLLM({"regions": ["광교"], "exclude_regions": [], "purpose": "카페"},
                      {"results": {"광교": {"seoul": False, "areas": [], "gu": []}}})
        p = run("광교 카페", llm, cand)
        expect("LLM 이 서울 밖이라 하면 필터 안 함·기록 안 함·서울 밖 안내",
               p.regions == [] and p.outside_regions == ["광교"] and not recorded("광교"), f"parsed={p}")

        # 7. 맵에 서울 밖으로 적힌 지명은 LLM 에 묻지 않고 서울 밖으로 알린다
        llm = FakeLLM({"regions": ["판교"], "exclude_regions": [], "purpose": "디저트"})
        p = run("판교 디저트 카페", llm, cand)
        expect("맵의 서울 밖 지명은 LLM 없이 안내", llm.calls == ["parse"] and p.outside_regions == ["판교"]
               and p.regions == [], f"calls={llm.calls} parsed={p}")

        # 8. 서울 밖 + 서울 지역 → 서울 지역으로만 거르고 서울 밖도 알린다
        llm = FakeLLM({"regions": ["판교", "성수"], "exclude_regions": [], "purpose": "작업"})
        p = run("판교나 성수에서 작업", llm, cand)
        expect("서울 밖 + 서울 지역", p.regions == ["성수동"] and p.outside_regions == ["판교"], f"parsed={p}")

        # 9. "서울"은 서울 밖이 아니다 (안내 없음)
        llm = FakeLLM({"regions": ["서울"], "exclude_regions": [], "purpose": "대형 카페"})
        p = run("서울 대형 카페", llm, cand)
        expect("서울은 안내 없음", p.regions == [] and p.outside_regions == [], f"parsed={p}")

        # 10. 제외 지역이 서울 밖이면 안내하지 않는다 (어차피 서울 카페만 있다)
        llm = FakeLLM({"regions": [], "exclude_regions": ["부산"], "purpose": "카페"})
        p = run("부산 말고 카페", llm, cand)
        expect("서울 밖 제외 지역은 안내 없음", p.outside_regions == [] and p.exclude_regions == [], f"parsed={p}")

        # 11. 서울 안인 건 확실한데 동·구는 모름 → 거르지 않고 후보로 기록(사람이 채운다)
        llm = FakeLLM({"regions": ["골목길카페촌"], "exclude_regions": [], "purpose": "카페"},
                      {"results": {"골목길카페촌": {"seoul": True, "areas": [], "gu": []}}})
        p = run("골목길카페촌 카페", llm, cand)
        expect("서울 안·위치 모름은 필터 안 함, 후보 기록",
               p.regions == [] and p.outside_regions == [] and recorded("골목길카페촌"), f"parsed={p}")

        # 12. 서울 안인지 모름(seoul=null) → 거르지도, 기록하지도, 안내하지도 않는다
        llm = FakeLLM({"regions": ["어딘지몰라"], "exclude_regions": [], "purpose": "카페"},
                      {"results": {"어딘지몰라": {"seoul": None, "areas": [], "gu": []}}})
        p = run("어딘지몰라 카페", llm, cand)
        expect("서울인지 모르면 기록·안내 없음",
               p.regions == [] and p.outside_regions == [] and not recorded("어딘지몰라"), f"parsed={p}")

        # 13. 지역 확인 호출 실패 → 거르지도, 기록하지도 않는다
        llm = FakeLLM({"regions": ["새동네"], "exclude_regions": [], "purpose": "카페"}, TimeoutError("timeout"))
        p = run("새동네 카페", llm, cand)
        expect("지역 확인 실패면 필터 안 함, 기록 안 함",
               p.unmatched_regions == ["새동네"] and p.outside_regions == [] and not recorded("새동네"), f"parsed={p}")

        # 14. seoul 을 빠뜨린 답은 동·구가 있으면 서울 안으로 본다
        llm = FakeLLM({"regions": ["망원한강공원"], "exclude_regions": [], "purpose": "카페"},
                      {"results": {"망원한강공원": {"areas": ["망원동"], "gu": ["마포구"]}}})
        p = run("망원한강공원 카페", llm, cand)
        expect("seoul 누락 + 동·구 있음 → 서울 안", p.regions == ["마포구"] and recorded("망원한강공원"), f"parsed={p}")

        # 8. 1차 호출 실패 → 질문 전체를 조건으로
        p = run("망원동 커피", FakeLLM(TimeoutError("timeout")), cand)
        expect("1차 호출 실패면 질문 전체가 조건", p == ParsedQuery(purpose="망원동 커피"), f"parsed={p}")

    return failures


def main() -> int:
    failures = check_map() + check_flow()
    total = len(MAP_CASES) + len(OUTSIDE_CASES) + FLOW_CHECKS
    print(f"\n{total - failures}/{total} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
