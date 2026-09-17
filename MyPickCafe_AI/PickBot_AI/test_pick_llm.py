"""
픽봇 LLM 2차 호출(후보 카페 중 추천 선택) 테스트 — 서버 없이 직접 호출

- 서비스와 같은 시스템 프롬프트(_SYSTEM_PROMPT), 사용자 메시지(build_pick_user_message),
  반환 카페 선택 규칙(select_picks: 요구사항 모두 충족한 카페 전부, 없으면 유사도 1위만)을 쓴다.
- 후보 카페는 실제 흐름대로(지역 필터 → 리뷰 벡터 검색) 한 번만 만들어 모든 모델에 똑같이 넘긴다.
- LLM 은 재시도 없이 직접 호출해 실패를 그대로 센다.

채점 (정답 라벨이 없어 관련성·근거성은 근사치)
- 형식: JSON 실패, 무시한 응답 항목(후보에 없는 cafeId·중복·형식 오류), 추천 수, 유사도 1위 대체 횟수
- 관련성: 추천한 카페의 리뷰에 조건 핵심어가 있는지 ("주차 불가" 같은 부정 표현은 제외)
- 근거성: 추천 이유 문장의 글자 2-gram 중 그 카페 리뷰에도 있는 비율

사용법 (MyPickCafe_AI/ 에서, DB_PASSWORD·LLM_API_KEY 필요, Ollama 실행 중)
  python PickBot_AI/test_pick_llm.py                              # .env 의 LLM_MODEL
  python PickBot_AI/test_pick_llm.py glm-5p3-flash gpt-oss-120b     # 모델 여러 개 비교
"""

from __future__ import annotations
import asyncio
import json
import statistics
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
sys.stdout.reconfigure(encoding="utf-8")

import httpx

import pickbot_rag
from config import Settings
from query_parser import ParsedQuery

settings = Settings()
MODEL_PREFIX = "accounts/fireworks/models/"
PRICES = {  # Fireworks Standard (USD / 1M tokens): 입력, 캐시 입력, 출력
    "deepseek-v4p1-flash":            (0.22, 0.007, 0.66),
    "glm-5p3-flash":                  (0.15, 0.03,  0.50),
    "nemotron-lightning-3p5-30b-a3b": (0.05, 0.01,  0.20),
    "gpt-oss-120b":                   (0.15, 0.015, 0.60),
    "qwen3p8-max":                    (2.00, 0.25,  6.00),
}

# (조건 문장, 포함 지역(None=필터 없음), 관련 핵심어(하나라도), 부정 표현(있으면 관련 없음))
PARKING_NEG = ["주차 불가", "주차가 불가", "주차 안", "주차가 안", "주차는 안", "주차 공간이 없", "주차장이 없",
               "주차가 어렵", "주차 어렵", "주차는 어렵", "주차가 힘들", "주차 힘들", "주차는 힘들", "주차 불편", "주차가 불편"]
CASES = [
    ("노트북 하기 좋은 조용한 카페",      None,                 ["노트북", "콘센트", "작업", "공부", "조용"], []),
    ("주차 가능한 카페",                 ["마포구"],           ["주차"], PARKING_NEG),
    ("디저트가 맛있는 카페",              ["강남구", "잠실동"], ["디저트", "케이크", "마카롱", "쿠키", "휘낭시에", "스콘", "크로플", "타르트", "빵"], []),
    ("반려견 동반 가능한 카페",           None,                 ["반려", "강아지", "애견", "댕댕"], ["동반 불가", "출입 불가", "입장 불가"]),
    ("루프탑이 있는 카페",               ["여의도동"],         ["루프탑", "옥상", "테라스"], []),
    ("사진 찍기 좋은 카페",               None,                 ["사진", "포토", "인스타", "감성"], []),
    ("커피 맛이 진한 카페",               ["연희동"],           ["진한", "진하", "원두", "산미", "고소", "쌉쌀", "바디감"], []),
    ("브런치 메뉴가 있는 카페",           None,                 ["브런치", "샌드위치", "파스타", "토스트", "에그", "샐러드"], []),
    ("아이랑 가기 좋은 카페",             ["불광동"],           ["아이", "아기", "키즈", "유모차", "가족"], []),
    ("창가 자리가 넓고 채광 좋은 카페",    None,                 ["창가", "채광", "햇살", "통창", "창문", "햇빛"], []),
]


def relevant(review: str, keywords: list[str], negatives: list[str]) -> bool:
    return any(k in review for k in keywords) and not any(n in review for n in negatives)


def grounding(snippet: str, review: str) -> float:
    grams = {snippet[i:i + 2] for i in range(len(snippet) - 1) if not snippet[i:i + 2].isspace()}
    if not grams:
        return 0.0
    return sum(g in review for g in grams) / len(grams)


def call(model: str, user_message: str) -> dict:
    """서비스의 2차 호출과 같은 요청 조건(출력 한도 포함). 재시도 없음."""
    body = {
        "model": model,
        "messages": [{"role": "system", "content": pickbot_rag._SYSTEM_PROMPT},
                     {"role": "user", "content": user_message}],
        "stream": False,
        "response_format": {"type": "json_object"},
        "temperature": 0.0,
        "top_p": 0.9,
        "max_tokens": pickbot_rag._PICK_MAX_TOKENS,  # 서비스의 2차 호출 한도와 같게
    }
    headers = {"Authorization": f"Bearer {settings.llm_api_key}"} if settings.llm_api_key else {}
    t = time.perf_counter()
    try:
        r = httpx.post(f"{settings.llm_base_url.rstrip('/')}/chat/completions", json=body, headers=headers, timeout=180)
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
    directory = await rag._cafe_directory()
    inputs = []
    for purpose, regions, keywords, negatives in CASES:
        ids = None
        if regions:
            ids = [str(c["cafe_id"]) for c in pickbot_rag._filter_by_region(directory, ParsedQuery(regions=regions))]
        top = await rag._search_top_cafes(purpose, ids, 5)
        inputs.append({"purpose": purpose, "regions": regions, "keywords": keywords, "negatives": negatives,
                       "top": top, "message": pickbot_rag.build_pick_user_message(purpose, top)})
    return inputs


def run_model(model: str, inputs: list[dict]) -> dict:
    name = model.removeprefix(MODEL_PREFIX)
    print(f"\n{'=' * 78}\n모델: {name}\n{'=' * 78}")
    recs, picked_total, rel_picked, rel_available, rel_hit = [], 0, 0, 0, 0
    ignored = fallback = 0
    grounds = []
    for i, inp in enumerate(inputs, 1):
        rec = call(model, inp["message"])
        recs.append(rec)
        by_id = {c["cafe_id"]: c for c in inp["top"]}
        rel_ids = {cid for cid, c in by_id.items() if relevant(c["review"], inp["keywords"], inp["negatives"])}
        rel_available += len(rel_ids)
        region = f" [{'/'.join(inp['regions'])}]" if inp["regions"] else ""
        if not rec["ok"]:
            print(f"[{i:02d}] ✗ 실패 {rec['sec']:.1f}s — {inp['purpose']}{region}\n      {rec['err']}")
            continue

        picks, stats = pickbot_rag.select_picks(rec["raw"], inp["top"])
        ignored += stats["ignored"]
        fallback += stats["fallback_top1"]
        seen = {p["cafeId"] for p in picks}
        lines = []
        for p in picks:
            is_rel = p["cafeId"] in rel_ids
            g = grounding(p["snippet"], by_id[p["cafeId"]]["review"])
            grounds.append(g)
            v = stats["verdicts"].get(p["cafeId"], {})
            lines.append(f"      {'●' if is_rel else '○'} [{p['cafeId']}] 근거 {g:.2f} | 누락={v.get('missing')} | {p['snippet']}")
        picked_total += len(seen)
        rel_picked += len(seen & rel_ids)
        rel_hit += len(seen & rel_ids)
        tag = "  (모두 충족 없음 → 유사도 1위만)" if stats["fallback_top1"] else ""
        print(f"[{i:02d}] {rec['sec']:4.1f}s  {inp['purpose']}{region}  요구사항={stats['requirements']}")
        print(f"      → 반환 {len(seen)}곳{tag} (관련 {len(seen & rel_ids)} / 후보 중 관련 {len(rel_ids)})")
        print("\n".join(lines))

    ok_secs = [r["sec"] for r in recs if r["ok"]]
    price = PRICES.get(name)
    per1k = None
    if price:
        total = sum(((r.get("pt", 0) - r.get("cached", 0)) * price[0] + r.get("cached", 0) * price[1]
                     + r.get("ct", 0) * price[2]) / 1e6 for r in recs)
        per1k = total / len(recs) * 1000
    return {
        "model": name, "n": len(inputs), "fail": sum(not r["ok"] for r in recs),
        "length": sum(r.get("finish") == "length" for r in recs),
        "ignored": ignored, "fallback": fallback,
        "avg_picks": picked_total / max(len(ok_secs), 1),
        "precision": rel_picked / picked_total if picked_total else 0.0,
        "recall": rel_hit / rel_available if rel_available else 0.0,
        "ground": statistics.mean(grounds) if grounds else 0.0,
        "avg": statistics.mean(ok_secs) if ok_secs else 0.0,
        "max": max(ok_secs) if ok_secs else 0.0,
        "reason": statistics.mean([r.get("reason", 0) for r in recs if r["ok"]]) if ok_secs else 0.0,
        "per1k": per1k,
    }


def main() -> None:
    models = [m if m.startswith("accounts/") else MODEL_PREFIX + m for m in sys.argv[1:]] or [settings.llm_model]
    rag = pickbot_rag.CafeRAG(settings)
    inputs = asyncio.run(build_inputs(rag))
    print("후보 카페 준비 완료 (모든 모델에 같은 입력):")
    for inp in inputs:
        rel = sum(relevant(c["review"], inp["keywords"], inp["negatives"]) for c in inp["top"])
        print(f"  - {inp['purpose']:<22} 지역={inp['regions'] or '-'}  후보 {len(inp['top'])}곳 (리뷰에 핵심어 있는 곳 {rel})")

    summary = [run_model(m, inputs) for m in models]

    print(f"\n{'=' * 78}\n요약 ({len(inputs)}건, 관련성·근거성은 핵심어 기반 근사치)\n{'=' * 78}")
    print(f"{'모델':<32} {'실패':>4} {'한도':>4} {'무시항목':>6} {'1위대체':>6} {'평균추천':>6} "
          f"{'관련정밀':>6} {'관련재현':>6} {'근거':>5} {'평균s':>6} {'최대s':>6} {'추론토큰':>6} {'1천건$':>7}")
    for s in summary:
        per1k = f"{s['per1k']:.2f}" if s["per1k"] is not None else "-"
        print(f"{s['model']:<32} {s['fail']:>4} {s['length']:>4} {s['ignored']:>6} {s['fallback']:>6} "
              f"{s['avg_picks']:6.1f} {s['precision']:6.2f} {s['recall']:6.2f} {s['ground']:5.2f} "
              f"{s['avg']:6.1f} {s['max']:6.1f} {s['reason']:6.0f} {per1k:>7}")


if __name__ == "__main__":
    main()
