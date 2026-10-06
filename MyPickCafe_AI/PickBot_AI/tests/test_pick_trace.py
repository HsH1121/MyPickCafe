"""
픽봇 단계별 추적 로그(PICKBOT_TRACE) 테스트 — LLM·DB·ChromaDB·Ollama 없이 실행된다.

recommend() 를 실제 코드 그대로 돌리고, 외부 호출(카페 목록·임베딩·ChromaDB·LLM)만 가짜로 바꾼다.
- 켜져 있으면 요청마다 ①~⑤ 단계가 시간과 함께 한 덩어리로 남는다.
- 꺼져 있으면 아무것도 남지 않고 로그 파일도 만들지 않는다.
- LLM 재시도·실패, 맵에 없는 지역의 LLM 확인, 조건 없는 질문의 건너뜀이 표시된다.

사용법 (MyPickCafe_AI/ 에서)
  python PickBot_AI/tests/test_pick_trace.py
"""

from __future__ import annotations
import asyncio
import logging
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

import httpx

import pick_trace
import pickbot_rag
import query_parser
from config import Settings

DIRECTORY = [
    {"cafe_id": 1, "cafe_name": "연남A", "address": "서울시 마포구 연남동 1-1", "review_count": 3, "good_count": 2},
    {"cafe_id": 2, "cafe_name": "연남B", "address": "서울시 마포구 연남동 2-2", "review_count": 2, "good_count": 1},
    {"cafe_id": 3, "cafe_name": "성수C", "address": "서울시 성동구 성수동 3-3", "review_count": 5, "good_count": 4},
]
REVIEWS = {1: "테라스가 넓어요", 2: "조용하고 테라스 있어요", 3: "테라스 좋아요"}


class FakeCollection:
    def count(self) -> int:
        return 3

    def query(self, query_embeddings, n_results, where=None, include=None):
        ids = [1, 2, 3]
        if where:
            conds = where.get("$and", [where])
            for c in conds:
                rule = c["cafe_id"]
                if "$in" in rule:
                    ids = [i for i in ids if str(i) in rule["$in"]]
                if "$nin" in rule:
                    ids = [i for i in ids if str(i) not in rule["$nin"]]
        ids = ids[:n_results]
        metas = [{"cafe_id": str(i), "cafe_name": DIRECTORY[i - 1]["cafe_name"],
                  "address": DIRECTORY[i - 1]["address"], "review": REVIEWS[i]} for i in ids]
        n = len(query_embeddings)
        return {"ids": [[f"review_{i}" for i in ids]] * n, "metadatas": [metas] * n,
                "distances": [[0.3 + 0.01 * i for i in ids]] * n,
                "embeddings": [[[1.0, 0.1 * i] for i in ids]] * n}


class FakeEmbedding:
    def __call__(self, texts: list[str]) -> list[list[float]]:
        return [[1.0, 0.0] for _ in texts]


class FakeLLM:
    """프롬프트로 호출 종류를 구분해 정해진 답을 준다. 'fail' 이면 attempt_log 에 재시도 2번 실패를 남기고 예외."""

    def __init__(self, parse, resolve=None, pick=None) -> None:
        self.answers = {"parse": parse, "resolve": resolve, "pick": pick}

    async def __call__(self, **kw):
        if kw["system_prompt"] is query_parser.SYSTEM_PROMPT:
            kind = "parse"
        elif kw["system_prompt"] is query_parser.RESOLVE_SYSTEM_PROMPT:
            kind = "resolve"
        else:
            kind = "pick"
        answer = self.answers[kind]
        log = kw.get("attempt_log")
        if answer == "fail":
            if log is not None:
                log += [{"sec": 5.0, "error": "ReadTimeout"}, {"sec": 5.0, "error": "ReadTimeout"}]
            raise httpx.ReadTimeout("timeout")
        if answer == "retry_ok":
            answer = self.answers[kind + "_after_retry"]
            if log is not None:
                log += [{"sec": 5.0, "error": "ReadTimeout"}, {"sec": 1.0, "error": None}]
        elif log is not None:
            log.append({"sec": 0.1, "error": None})
        return answer


class Capture(logging.Handler):
    def __init__(self) -> None:
        super().__init__()
        self.messages: list[str] = []

    def emit(self, record: logging.LogRecord) -> None:
        self.messages.append(record.getMessage())


def make_rag(settings: Settings) -> pickbot_rag.CafeRAG:
    rag = object.__new__(pickbot_rag.CafeRAG)
    rag.settings = settings
    rag._col = FakeCollection()
    rag._emb_fn = FakeEmbedding()
    rag._directory = DIRECTORY
    rag._directory_loaded_at = 1e18  # 캐시가 늘 유효하게
    pickbot_rag.fetch_representative_reviews = lambda settings, ids: {i: REVIEWS[i] for i in ids}
    return rag


def run(llm: FakeLLM, enabled: bool, query: str, capture: Capture, tmp: Path) -> tuple[dict, str]:
    query_parser.call_llm = llm
    pickbot_rag.call_llm = llm
    settings = Settings(pickbot_trace=enabled, region_candidates_path=str(tmp / "candidates.json"),
                        llm_reasoning_effort="")
    before = len(capture.messages)
    result = asyncio.run(make_rag(settings).recommend(query))
    return result, "\n".join(capture.messages[before:])


PICK_ALL = {"requirements": ["테라스 있음"], "cafes": [
    {"cafeId": 1, "matched": ["테라스 있음"], "missing": [], "snippet": "테라스가 넓어요"},
    {"cafeId": 2, "matched": ["테라스 있음"], "missing": [], "snippet": "테라스 있어요"},
]}


def main() -> int:
    failures = 0

    def expect(name: str, ok: bool, log: str) -> None:
        nonlocal failures
        failures += not ok
        print(f"[{'OK ' if ok else 'FAIL'}] {name}" + ("" if ok else "\n" + log))

    capture = Capture()
    pick_trace.logger.addHandler(capture)
    pick_trace.logger.propagate = False  # 테스트 출력에 로그 본문을 섞지 않는다

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        pick_trace.LOG_PATH = tmp / "pickbot_trace.log"

        # 1. 꺼져 있으면 아무것도 남지 않는다
        llm = FakeLLM({"regions": ["연트럴파크"], "exclude_regions": [], "purpose": "테라스 카페"}, pick=PICK_ALL)
        _, log = run(llm, False, "연트럴파크 테라스 카페", capture, tmp)
        expect("꺼져 있으면 기록 없음, 로그 파일도 안 만듦", log == "" and not pick_trace.LOG_PATH.exists(), log)

        # 2. 켜져 있으면 전체 단계
        result, log = run(llm, True, "연트럴파크 테라스 카페", capture, tmp)
        steps = ["카페 목록", "① 질문 분해 LLM", "② 지역 매핑", "연트럴파크 → ['연남동']", "지역 확인 LLM", "호출 안 함",
                 "③ 주소 필터", "후보 2곳", "④ 조건 임베딩", "벡터 검색", "[1] 연남A", "⑤ 판정 LLM", "모두 충족 2곳",
                 "2곳 반환"]
        expect("켜져 있으면 ①~⑤ 단계와 결과", all(s in log for s in steps) and len(result["results"]) == 2, log)
        expect("로그 파일에도 기록", pick_trace.LOG_PATH.exists()
               and "① 질문 분해 LLM" in pick_trace.LOG_PATH.read_text(encoding="utf-8"), log)

        # 3. 맵에 없는 지역 → 지역 확인 LLM
        llm = FakeLLM({"regions": ["합정카페거리"], "exclude_regions": [], "purpose": "테라스 카페"},
                      resolve={"results": {"합정카페거리": {"seoul": True, "areas": ["합정동"], "gu": ["마포구"]}}},
                      pick=PICK_ALL)
        _, log = run(llm, True, "합정카페거리 테라스", capture, tmp)
        expect("맵에 없는 지역은 지역 확인 LLM 과 답 표시",
               "합정카페거리 → 맵에 없음" in log
               and "LLM 답 서울=True 동=['합정동'] 구=['마포구'] → 서울 → 필터 ['마포구'], 후보 기록" in log, log)

        # 3-1. 서울 밖 지명 → 안내 표시, 결과는 전체에서
        llm = FakeLLM({"regions": ["판교"], "exclude_regions": [], "purpose": "테라스 카페"}, pick=PICK_ALL)
        result, log = run(llm, True, "판교 테라스 카페", capture, tmp)
        expect("서울 밖 지명은 안내 표시", "판교 → 서울 밖 (필터 안 함, 안내)" in log
               and "안내 OUTSIDE_SEOUL" in log and result["notice"] == "OUTSIDE_SEOUL" and result["results"], log)

        # 4. 1차 호출 재시도 후 성공
        llm = FakeLLM("retry_ok", pick=PICK_ALL)
        llm.answers["parse_after_retry"] = {"regions": [], "exclude_regions": [], "purpose": "테라스 카페"}
        _, log = run(llm, True, "테라스 카페", capture, tmp)
        expect("재시도는 시도별 시간·오류 표시", "시도1 5.0s ReadTimeout, 시도2 1.0s 성공" in log
               and "지역 조건 없음 → 전체 3곳" in log, log)

        # 5. 1차 호출 실패 → 질문 전체가 조건
        _, log = run(FakeLLM("fail", pick=PICK_ALL), True, "망원동 테라스", capture, tmp)
        expect("1차 실패는 대체 경로 표시", "실패 (시도1 5.0s ReadTimeout, 시도2 5.0s ReadTimeout)" in log
               and "질문 전체를 조건으로" in log, log)

        # 6. 판정 호출 실패
        llm = FakeLLM({"regions": [], "exclude_regions": [], "purpose": "테라스 카페"}, pick="fail")
        _, log = run(llm, True, "테라스 카페", capture, tmp)
        expect("판정 실패는 검색 결과 그대로 반환 표시", "검색 결과 그대로 반환" in log and "3곳 반환" in log, log)

        # 7. 조건 없는 질문 → ④·⑤ 건너뜀
        llm = FakeLLM({"regions": ["성수"], "exclude_regions": [], "purpose": ""})
        _, log = run(llm, True, "성수 카페", capture, tmp)
        expect("지역만 있으면 ④·⑤ 건너뜀, 긍정 리뷰 순위", "건너뜀 (조건 문장 없음)" in log
               and "긍정 리뷰 순위" in log and "[3] 성수C (긍정 4/5)" in log, log)

        # 8. 제외로 전부 빠지면 안내
        llm = FakeLLM({"regions": [], "exclude_regions": ["마포구", "성수"], "purpose": "카페"})
        result, log = run(llm, True, "마포구랑 성수 빼고", capture, tmp)
        expect("지역 없음 안내 표시", "후보 0곳" in log and "안내 REGION_NOT_FOUND" in log, log)

        pick_trace.logger.removeHandler(pick_trace._file_handler)
        pick_trace._file_handler.close()

    total = 10
    print(f"\n{total - failures}/{total} 통과")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
