"""
픽봇 지역 맵·질문 분해 흐름 테스트 — LLM·DB 없이 실행된다.

- 지역 맵: 지역 표현(역·별칭·줄임말·"근처" 등)을 허용 지역(카페 주소의 구·동 이름)으로 바꾸는지
- 질문 분해: 맵에 있으면 LLM 1차 호출만, 맵에 없는 표현만 LLM 에 동·구를 묻고 후보 파일에 기록하는지
- 지역 확인 호출이 실패하면 그 표현은 지역 필터에 쓰지 않는지

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


def check_map() -> int:
    failures = 0
    for expr, expected in MAP_CASES:
        got = query_parser.REGION_MAP.resolve(expr, ALLOWED)
        ok = got == expected
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] 맵 {expr!r} → {got}" + ("" if ok else f"  (기대 {expected})"))
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


def check_flow() -> int:
    failures = 0

    def expect(name: str, ok: bool, detail: str) -> None:
        nonlocal failures
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
                      {"results": {"합정카페거리": {"areas": ["합정동", "서교동"], "gu": ["마포구"]}}})
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
                      {"results": {"성수연방": {"areas": ["성수동2가"], "gu": ["성동구"]}}})
        p = run("성수연방 카페", llm, cand)
        expect("법정동 N가 → 동 이름", p.regions == ["성수동"], f"parsed={p}")

        # 6. LLM 이 서울 밖이라고 답하면 지역 필터 없음(목록 밖 지역)
        llm = FakeLLM({"regions": ["광교"], "exclude_regions": [], "purpose": "카페"},
                      {"results": {"광교": {"areas": [], "gu": []}}})
        p = run("광교 카페", llm, cand)
        expect("서울 밖이면 목록 밖 지역", p.regions == [] and p.unmatched_regions == ["광교"], f"parsed={p}")

        # 7. 지역 확인 호출 실패 → 지역 필터 없이, 후보는 기록(LLM 답 없음)
        llm = FakeLLM({"regions": ["새동네"], "exclude_regions": [], "purpose": "카페"}, TimeoutError("timeout"))
        p = run("새동네 카페", llm, cand)
        item = json.loads(cand.read_text(encoding="utf-8")).get("새동네", {})
        expect("지역 확인 실패면 목록 밖 지역, 후보는 기록",
               p.unmatched_regions == ["새동네"] and item.get("count") == 1 and "llm" not in item,
               f"parsed={p} 후보={item}")

        # 8. 1차 호출 실패 → 질문 전체를 조건으로
        p = run("망원동 커피", FakeLLM(TimeoutError("timeout")), cand)
        expect("1차 호출 실패면 질문 전체가 조건", p == ParsedQuery(purpose="망원동 커피"), f"parsed={p}")

    return failures


def main() -> int:
    total = len(MAP_CASES) + 9
    failures = check_map() + check_flow()
    print(f"\n{total - failures}/{total} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
