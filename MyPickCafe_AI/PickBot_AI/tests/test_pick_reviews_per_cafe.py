"""
픽봇 LLM 2차 호출에 카페당 리뷰를 1·2·3개 넣었을 때의 비용·소요 시간 비교 — 서버 없이 직접 호출

요구사항이 여러 개인 질문("노트북 하기 좋고 주차 편한 카페")은 카페당 리뷰 1개로는 일부 요구사항이
언급되지 않아 미충족으로 판정되기 쉽다. 카페당 리뷰를 늘렸을 때 비용·지연이 얼마나 느는지 잰다.

- 후보 카페 5곳은 서비스와 같은 벡터 검색(_rag_cafes)으로 한 번만 고르고, 세 조건에 똑같이 쓴다.
- 카페마다 같은 조건 문장 임베딩으로 그 카페 리뷰만 검색해 유사도 상위 k개를 넣는다 (방식 A).
  k=1 은 서비스와 같은 메시지(build_pick_user_message)라 현재 동작의 기준값이다.
- 모델은 배포 환경과 같게 고정한다: Fireworks glm-5p3-flash, reasoning_effort=low,
  출력 한도 _PICK_MAX_TOKENS. 로컬 .env 의 LLM_MODEL·LLM_BASE_URL 은 쓰지 않는다.
- LLM 은 재시도 없이 직접 호출해 실패를 그대로 센다. 같은 질문의 k=1·2·3 을 번갈아 호출해
  시간대에 따른 지연 차이가 한쪽에 몰리지 않게 한다.

2026-10-06 조건별 검색(test_condition_search.py)이 이 방식을 대체했다. 서비스의 판정 프롬프트·메시지가
요구사항 목록을 받는 형식으로 바뀌어, 이 스크립트의 k=1 은 바뀌기 전 메시지를 직접 만든다. 측정 결과는
test_pick_reviews_per_cafe_result.txt 에 당시 기준으로 남긴다.

함께 보는 값 (정답 라벨이 없어 핵심어 기반 근사치)
- 리뷰 커버: 후보 카페의 요구사항 중, 넣은 리뷰에 핵심어가 있는 요구사항 비율과 모두 커버한 카페 수
- 모두 충족: LLM 이 요구사항을 모두 충족했다고 판정한 카페 수

사용법 (MyPickCafe_AI/ 에서, Ollama 실행 중, Fireworks 키 필요 — .env 의 키는 서버 전용이라 주석 처리돼 있다)
  $env:LLM_API_KEY = "fw_..."
  python PickBot_AI/tests/test_pick_reviews_per_cafe.py       # 질문마다 k별 1회
  python PickBot_AI/tests/test_pick_reviews_per_cafe.py 3     # 질문마다 k별 3회 반복
"""

from __future__ import annotations
import asyncio
import json
import statistics
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
sys.stdout.reconfigure(encoding="utf-8")

import httpx

import pickbot_rag
from config import Settings

settings = Settings()
BASE_URL = "https://api.fireworks.ai/inference/v1"
MODEL = "accounts/fireworks/models/glm-5p3-flash"
REASONING_EFFORT = "low"
PRICE = (0.15, 0.03, 0.50)  # Fireworks Standard (USD / 1M tokens): 입력, 캐시 입력, 출력
TOP_CAFES = 5
KS = (1, 2, 3)

PARKING_NEG = ["주차 불가", "주차가 불가", "주차 안", "주차가 안", "주차는 안", "주차 공간이 없", "주차장이 없",
               "주차가 어렵", "주차 어렵", "주차는 어렵", "주차가 힘들", "주차 힘들", "주차는 힘들", "주차 불편", "주차가 불편"]
LAPTOP = (["노트북", "콘센트", "작업", "공부", "카공"], [])
PARKING = (["주차"], PARKING_NEG)
QUIET = (["조용", "한적", "차분"], ["시끄럽", "시끄러"])
DESSERT = (["디저트", "케이크", "마카롱", "쿠키", "휘낭시에", "스콘", "크로플", "타르트", "빵"], [])
PET = (["반려", "강아지", "애견", "댕댕"], ["동반 불가", "출입 불가", "입장 불가"])
PHOTO = (["사진", "포토", "인스타", "감성"], [])
BRUNCH = (["브런치", "샌드위치", "파스타", "토스트", "에그", "샐러드"], [])
KIDS = (["아이", "아기", "키즈", "유모차", "가족"], [])
ROOFTOP = (["루프탑", "옥상", "테라스"], [])
COFFEE = (["원두", "산미", "고소", "진한", "진하", "바디감", "커피 맛"], [])
WINDOW = (["창가", "채광", "햇살", "통창", "창문", "햇빛"], [])

# (조건 문장, 요구사항별 (핵심어, 부정 표현))
CASES = [
    ("노트북 하기 좋고 주차 편한 카페",          [LAPTOP, PARKING]),
    ("조용하고 디저트가 맛있는 카페",            [QUIET, DESSERT]),
    ("반려견 동반 가능하고 주차되는 카페",        [PET, PARKING]),
    ("사진 찍기 좋고 브런치 메뉴가 있는 카페",     [PHOTO, BRUNCH]),
    ("아이랑 가기 좋고 주차 편한 카페",           [KIDS, PARKING]),
    ("루프탑이 있고 커피 맛이 좋은 카페",         [ROOFTOP, COFFEE]),
    ("창가 자리 채광 좋고 디저트 맛있는 카페",     [WINDOW, DESSERT]),
    ("조용하고 노트북 하기 좋고 주차 편한 카페",   [QUIET, LAPTOP, PARKING]),
]


def covers(text: str, requirement: tuple[list[str], list[str]]) -> bool:
    keywords, negatives = requirement
    return any(k in text for k in keywords) and not any(n in text for n in negatives)


def build_message(purpose: str, top: list[dict], k: int) -> str:
    """k=1 은 조건별 검색 전의 서비스 메시지, k>=2 는 카페마다 리뷰를 번호 붙여 나열한다."""
    if k == 1:
        context = "\n\n".join(f"[카페{i}] ID={c['cafe_id']}\n리뷰: {c['review']}" for i, c in enumerate(top, 1))
    else:
        context = "\n\n".join(
            f"[카페{i}] ID={c['cafe_id']}\n"
            + "\n".join(f"리뷰{j}: {r}" for j, r in enumerate(c["top_reviews"][:k], 1))
            for i, c in enumerate(top, 1)
        )
    return (
        f"사용자 조건: {purpose}\n\n"
        f"검색된 카페 정보:\n{context}\n\n"
        "위 카페 각각의 리뷰가 사용자 조건의 요구사항을 모두 충족하는지 판정해 JSON으로 반환하세요."
    )


def call(client: httpx.Client, user_message: str) -> dict:
    """서비스의 2차 호출과 같은 요청 조건(배포 모델·추론량·출력 한도). 재시도 없음."""
    body = {
        "model": MODEL,
        "messages": [{"role": "system", "content": pickbot_rag._SYSTEM_PROMPT},
                     {"role": "user", "content": user_message}],
        "stream": False,
        "response_format": {"type": "json_object"},
        "temperature": 0.0,
        "top_p": 0.9,
        "max_tokens": pickbot_rag._PICK_MAX_TOKENS,
        "reasoning_effort": REASONING_EFFORT,
    }
    t = time.perf_counter()
    try:
        r = client.post(f"{BASE_URL}/chat/completions", json=body)
    except Exception as e:
        return {"ok": False, "sec": time.perf_counter() - t, "err": f"{type(e).__name__}: {e}"}
    sec = time.perf_counter() - t
    if r.status_code != 200:
        return {"ok": False, "sec": sec, "err": f"HTTP {r.status_code} {r.text[:150]}"}
    d = r.json()
    u = d.get("usage") or {}
    ch = d["choices"][0]
    rec = {"sec": sec, "finish": ch.get("finish_reason"),
           "pt": u.get("prompt_tokens", 0), "ct": u.get("completion_tokens", 0),
           "cached": (u.get("prompt_tokens_details") or {}).get("cached_tokens") or 0,
           "reason": (u.get("completion_tokens_details") or {}).get("reasoning_tokens") or 0}
    rec["cost"] = ((rec["pt"] - rec["cached"]) * PRICE[0] + rec["cached"] * PRICE[1] + rec["ct"] * PRICE[2]) / 1e6
    content = ch["message"].get("content")
    try:
        raw = json.loads(content)
        if not isinstance(raw, dict) or not isinstance(raw.get("cafes"), list):
            raise ValueError("cafes 가 리스트가 아님")
        rec.update(ok=True, raw=raw)
    except Exception:
        rec.update(ok=False, err=f"형식 실패 finish={rec['finish']} content={str(content)[:120]!r}")
    return rec


async def build_inputs(rag: pickbot_rag.CafeRAG) -> list[dict]:
    """질문마다 후보 카페 5곳(서비스와 같은 검색)과 카페별 유사도 상위 리뷰 max(KS)개."""
    inputs = []
    for purpose, requirements in CASES:
        top = (await rag._rag_cafes([purpose], None))[:TOP_CAFES]
        query_emb = await asyncio.to_thread(rag._emb_fn.embed_query, purpose)
        for c in top:
            res = rag._col.query(query_embeddings=[query_emb], n_results=max(KS),
                                 where={"cafe_id": str(c["cafe_id"])})
            c["top_reviews"] = [m["review"] for m in res["metadatas"][0]]
        inputs.append({"purpose": purpose, "requirements": requirements, "top": top,
                       "messages": {k: build_message(purpose, top, k) for k in KS}})
    return inputs


def coverage(inp: dict, k: int) -> tuple[int, int, int]:
    """(커버된 요구사항 수, 전체 요구사항 수, 모두 커버한 카페 수) — 카페마다 넣은 리뷰 k개 기준."""
    hit = total = full = 0
    for c in inp["top"]:
        text = "\n".join(c["top_reviews"][:k])
        n = sum(covers(text, req) for req in inp["requirements"])
        hit += n
        total += len(inp["requirements"])
        full += n == len(inp["requirements"])
    return hit, total, full


def main() -> None:
    if not settings.llm_api_key:
        sys.exit("LLM_API_KEY 가 없습니다. Fireworks 키를 환경변수로 지정하세요: $env:LLM_API_KEY = \"fw_...\"")
    repeat = int(sys.argv[1]) if len(sys.argv) > 1 else 1

    rag = pickbot_rag.CafeRAG(settings)
    inputs = asyncio.run(build_inputs(rag))

    print(f"모델: {MODEL.removeprefix('accounts/fireworks/models/')} (reasoning_effort={REASONING_EFFORT}), "
          f"질문 {len(inputs)}개 × k{list(KS)} × {repeat}회\n")
    print("후보 카페 준비 완료 (리뷰 커버 = 넣은 리뷰에 요구사항 핵심어가 있는 비율, 모두커버 = 카페 수):")
    for inp in inputs:
        cov = "  ".join(f"k={k} {h}/{t} 모두커버 {f}곳" for k in KS for h, t, f in [coverage(inp, k)])
        short = [len(c["top_reviews"]) for c in inp["top"] if len(c["top_reviews"]) < max(KS)]
        note = f"  (리뷰가 {max(KS)}개 미만인 카페 {len(short)}곳)" if short else ""
        print(f"  - {inp['purpose']:<24} {cov}{note}")

    recs: dict[int, list[dict]] = {k: [] for k in KS}
    full_match: dict[int, list[int]] = {k: [] for k in KS}
    with httpx.Client(timeout=180, headers={"Authorization": f"Bearer {settings.llm_api_key}"}) as client:
        for rnd in range(1, repeat + 1):
            print(f"\n{'=' * 78}\n{rnd}회차\n{'=' * 78}")
            for i, inp in enumerate(inputs, 1):
                print(f"[{i:02d}] {inp['purpose']}")
                for k in KS:
                    rec = call(client, inp["messages"][k])
                    recs[k].append(rec)
                    head = f"      k={k} ({k * len(inp['top']):>2}개) {rec['sec']:5.1f}s"
                    if not rec["ok"]:
                        print(f"{head}  ✗ {rec['err']}")
                        continue
                    _, stats = pickbot_rag.validate_llm_response_and_select_cafes(rec["raw"], inp["top"])
                    full_match[k].append(stats["full_match"])
                    print(f"{head}  입력 {rec['pt']:>5} (캐시 {rec['cached']:>5})  출력 {rec['ct']:>4} "
                          f"(추론 {rec['reason']:>4})  ${rec['cost']:.6f}  모두 충족 {stats['full_match']}곳")

    print(f"\n{'=' * 78}\n요약 (질문 {len(inputs)}개 × {repeat}회, 성공한 호출 기준 평균)\n{'=' * 78}")
    print(f"{'조건':<14} {'실패':>4} {'한도':>4} {'입력토큰':>8} {'캐시':>6} {'출력토큰':>8} {'추론':>6} "
          f"{'평균s':>6} {'중앙s':>6} {'최대s':>6} {'1회$':>9} {'1천건$':>7} {'리뷰커버':>8} {'모두충족':>8}")
    base = None
    for k in KS:
        ok = [r for r in recs[k] if r["ok"]]
        fail = len(recs[k]) - len(ok)
        length = sum(r.get("finish") == "length" for r in recs[k])
        if not ok:
            print(f"카페당 {k}개(총 {k * TOP_CAFES:>2}) {fail:>4} {length:>4}  성공한 호출 없음")
            continue
        avg = lambda key: statistics.mean(r[key] for r in ok)
        secs = [r["sec"] for r in ok]
        hit = sum(coverage(inp, k)[0] for inp in inputs)
        total = sum(coverage(inp, k)[1] for inp in inputs)
        cost = avg("cost")
        print(f"카페당 {k}개(총 {k * TOP_CAFES:>2}) {fail:>4} {length:>4} {avg('pt'):8.0f} {avg('cached'):6.0f} "
              f"{avg('ct'):8.0f} {avg('reason'):6.0f} {statistics.mean(secs):6.1f} {statistics.median(secs):6.1f} "
              f"{max(secs):6.1f} {cost:9.6f} {cost * 1000:7.3f} {hit / total:8.0%} "
              f"{statistics.mean(full_match[k]):8.1f}")
        if base is None:
            base = (cost, statistics.mean(secs))
        else:
            print(f"{'':<14} └ k=1 대비 비용 ×{cost / base[0]:.2f}, 평균 시간 ×{statistics.mean(secs) / base[1]:.2f}")


if __name__ == "__main__":
    main()
