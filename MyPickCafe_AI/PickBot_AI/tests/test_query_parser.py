"""
픽봇 질문 분해(LLM 1차 호출) 정확도 테스트 — 서버 없이 직접 호출

- query_parser 의 프롬프트·검증 로직을 그대로 쓰고, LLM 만 직접 호출한다(재시도 없음 → 실패를 그대로 센다).
- 퓨샷 예시와 겹치지 않는 질문으로 채점한다.
- 지역은 반환된 이름이 아니라 "실제로 걸러지는 카페 집합"으로 채점한다.
  현재 데이터에서 송파구와 잠실동처럼 같은 카페를 가리키는 값은 둘 다 정답이다.
- 여러 모델을 한 번에 비교할 수 있다.

사용법 (MyPickCafe_AI/ 에서, DB_PASSWORD·LLM_API_KEY 필요)
  python PickBot_AI/test_query_parser.py                         # .env 의 LLM_MODEL
  python PickBot_AI/test_query_parser.py glm-5p3-flash gpt-oss-120b  # 모델 여러 개 비교
  (accounts/fireworks/models/ 접두사는 생략 가능)
"""

from __future__ import annotations
import json
import statistics
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
sys.stdout.reconfigure(encoding="utf-8")

import httpx

import pickbot_rag
import query_parser
from config import Settings
from pickbot_db import fetch_cafe_directory

settings = Settings()
MODEL_PREFIX = "accounts/fireworks/models/"

# Fireworks Standard 요금 (USD / 1M tokens): 입력, 캐시 입력, 출력 — 모르는 모델은 비용을 표시하지 않는다
PRICES = {
    "deepseek-v4p1-flash":            (0.22, 0.007, 0.66),
    "glm-5p3-flash":                  (0.15, 0.03,  0.50),
    "nemotron-lightning-3p5-30b-a3b": (0.05, 0.01,  0.20),
    "gpt-oss-120b":                   (0.15, 0.015, 0.60),
    "qwen3p8-max":                    (2.00, 0.25,  6.00),
}

# ── 테스트 케이스 ────────────────────────────────────────────────────────────
# (질문, 포함 지역, 제외 지역, 데이터에 없는 지역을 말했는지, 조건 문장이 있어야 하는지(None=채점 안 함),
#  조건 문장에 남으면 안 되는 지역 표현, 조건 문장에 남아야 하는 핵심어)
CASES = [
    # 역·상권·통칭 → 동
    ("잠실역 근처 브런치 카페",                  {"잠실동"},   set(), False, True,  ["잠실"],        ["브런치"]),
    ("여의도에서 한강 보이는 카페",               {"여의도동"}, set(), False, True,  ["여의도"],      ["한강"]),
    ("가로수길 분위기 좋은 카페",                 {"신사동"},   set(), False, True,  ["가로수길"],    ["분위기"]),
    ("연트럴파크 근처 테라스 있는 카페",           {"연남동"},   set(), False, True,  ["연트럴파크"],  ["테라스"]),
    ("익선동 한옥 카페",                          {"익선동"},   set(), False, True,  ["익선동"],      ["한옥"]),
    ("천호역 쪽 24시간 카페",                     {"천호동"},   set(), False, True,  ["천호"],        ["24시간"]),
    ("불광동에 아이랑 가기 좋은 카페",             {"불광동"},   set(), False, True,  ["불광"],        ["아이"]),
    # 목록에 없는 동네 → 구
    ("망원동 커피 맛집",                          {"마포구"},   set(), False, True,  ["망원"],        ["커피"]),
    ("상수역 근처 디저트 카페",                   {"마포구"},   set(), False, True,  ["상수"],        ["디저트"]),
    ("강남역에서 소개팅하기 좋은 카페",            {"강남구"},   set(), False, True,  ["강남"],        ["소개팅"]),
    ("종로 조용한 전통찻집",                      {"종로구"},   set(), False, True,  ["종로"],        ["전통"]),
    # 구 이름 그대로
    ("강동구 반려견 동반 카페",                   {"강동구"},   set(), False, True,  ["강동"],        ["반려"]),
    ("은평구 대형 베이커리 카페",                 {"은평구"},   set(), False, True,  ["은평"],        ["베이커리"]),
    # 여러 지역 (OR)
    ("성수동이나 건대 쪽 빈티지한 카페",           {"성수동", "건대입구"}, set(), False, True, ["성수", "건대"], ["빈티지"]),
    # 제외
    ("강남 빼고 루프탑 카페",                     set(), {"강남구"},   False, True,  ["강남"],        ["루프탑"]),
    ("마포구 말고 다른 곳에서 공부할 카페",         set(), {"마포구"},   False, True,  ["마포"],        ["공부"]),
    ("홍대는 사람 많아서 싫고 연희동 쪽 조용한 카페", {"연희동"}, {"홍대입구", "연남동"}, False, True, ["홍대", "연희"], ["조용"]),
    # 데이터에 없는 지역
    ("제주도 오션뷰 카페",                        set(), set(), True,  True,  ["제주"],        ["오션뷰"]),
    ("분당 정자동 카페거리",                      set(), set(), True,  None,  ["분당", "정자"], []),
    ("인천 송도 뷰 좋은 카페",                    set(), set(), True,  True,  ["인천", "송도"], ["뷰"]),
    ("판교나 성수에서 작업하기 좋은 카페",          {"성수동"},   set(), True,  True,  ["판교", "성수"], ["작업"]),
    # 지역만 (조건 없음)
    ("잠실 카페",                                {"잠실동"},   set(), False, False, ["잠실"],        []),
    ("성수동 카페 알려줘",                        {"성수동"},   set(), False, False, ["성수"],        []),
    ("여의도나 이태원 카페 추천",                  {"여의도동", "이태원동"}, set(), False, False, ["여의도", "이태원"], []),
    # 지역 없음
    ("혼자 책 읽기 좋은 카페",                    set(), set(), False, True,  [],              ["책"]),
    ("반려견 동반 가능한 카페",                   set(), set(), False, True,  [],              ["반려"]),
    ("비 오는 날 창가 자리에서 쉬기 좋은 카페",     set(), set(), False, True,  [],              ["창가"]),
    ("서울 아무 데나 대형 카페",                  set(), set(), False, True,  [],              ["대형"]),
    ("카페 추천해줘",                            set(), set(), False, False, [],              []),
    # 지명처럼 보이지만 지역이 아님
    ("제주 말차 라떼 파는 카페",                  set(), set(), False, True,  [],              ["제주", "말차"]),
    ("뉴욕 치즈케이크 맛있는 곳",                  set(), set(), False, True,  [],              ["뉴욕", "치즈케이크"]),
]


def outcome(directory: list[dict], parsed: query_parser.ParsedQuery):
    """추천 흐름과 같은 규칙으로 '안내' 또는 '후보 카페 집합'을 계산한다."""
    if parsed.wants_region and not parsed.regions:
        return "REGION_NOT_FOUND"
    cafes = pickbot_rag._filter_by_region(directory, parsed)
    if (parsed.regions or parsed.exclude_regions) and not cafes:
        return "REGION_NOT_FOUND"
    return frozenset(c["cafe_id"] for c in cafes)


def call(model: str, system_prompt: str, query: str) -> dict:
    """shared/llm_client.py 와 같은 요청 조건. 재시도 없음."""
    body = {
        "model": model,
        "messages": [{"role": "system", "content": system_prompt},
                     {"role": "user", "content": f"질문: {query}"}],
        "stream": False,
        "response_format": {"type": "json_object"},
        "temperature": 0.0,
        "top_p": 0.9,
        "max_tokens": 1000,
    }
    headers = {"Authorization": f"Bearer {settings.llm_api_key}"} if settings.llm_api_key else {}
    t = time.perf_counter()
    try:
        r = httpx.post(f"{settings.llm_base_url.rstrip('/')}/chat/completions", json=body, headers=headers, timeout=120)
    except Exception as e:
        return {"ok": False, "sec": time.perf_counter() - t, "err": f"{type(e).__name__}: {e}"}
    sec = time.perf_counter() - t
    if r.status_code != 200:
        return {"ok": False, "sec": sec, "err": f"HTTP {r.status_code} {r.text[:150]}"}
    d = r.json()
    u = d.get("usage") or {}
    rec = {"sec": sec,
           "pt": u.get("prompt_tokens", 0), "ct": u.get("completion_tokens", 0),
           "cached": (u.get("prompt_tokens_details") or {}).get("cached_tokens") or 0,
           "reason": (u.get("completion_tokens_details") or {}).get("reasoning_tokens") or 0}
    content = d["choices"][0]["message"].get("content")
    try:
        raw = json.loads(content)
        if not isinstance(raw, dict):
            raise ValueError("객체가 아님")
        rec.update(ok=True, raw=raw)
    except Exception:
        rec.update(ok=False, err=f"JSON 실패 content={str(content)[:120]!r}")
    return rec


def run_model(model: str, directory: list[dict], allowed: list[str]) -> dict:
    system_prompt = query_parser.build_system_prompt(allowed)
    allowed_set = set(allowed)
    name = model.removeprefix(MODEL_PREFIX)
    print(f"\n{'=' * 78}\n모델: {name}\n{'=' * 78}")

    n = {"filter": 0, "purpose_has": 0, "purpose_has_total": 0, "clean": 0, "keyword": 0, "all": 0, "fail": 0}
    recs = []
    for i, (q, inc, exc, unmatched, has_purpose, region_words, keywords) in enumerate(CASES, 1):
        rec = call(model, system_prompt, q)
        recs.append(rec)
        if not rec["ok"]:
            n["fail"] += 1
            print(f"[{i:02d}] ✗ 호출 실패 {rec['sec']:.1f}s — {q}\n      {rec['err']}")
            continue

        got = query_parser._validate(rec["raw"], allowed_set)
        expected = query_parser.ParsedQuery(
            regions=sorted(inc), unmatched_regions=["(없는 지역)"] if unmatched else [],
            exclude_regions=sorted(exc), purpose="-")
        filter_ok = outcome(directory, got) == outcome(directory, expected)
        has_ok = True if has_purpose is None else (bool(got.purpose) == has_purpose)
        clean_ok = not any(w in got.purpose for w in region_words)
        kw_ok = all(k in got.purpose for k in keywords)
        all_ok = filter_ok and has_ok and clean_ok and kw_ok

        n["filter"] += filter_ok
        if has_purpose is not None:
            n["purpose_has"] += has_ok
            n["purpose_has_total"] += 1
        n["clean"] += clean_ok
        n["keyword"] += kw_ok
        n["all"] += all_ok

        mark = "✓" if all_ok else "✗"
        print(f"[{i:02d}] {mark} {rec['sec']:4.1f}s  {q}")
        if not all_ok:
            why = [label for ok, label in [(filter_ok, "지역 필터"), (has_ok, "조건 유무"),
                                            (clean_ok, "조건에 지역 섞임"), (kw_ok, "핵심어 누락")] if not ok]
            print(f"      틀린 항목: {', '.join(why)}")
            print(f"      응답: regions={got.regions} exclude={got.exclude_regions} "
                  f"unmatched={got.unmatched_regions} purpose={got.purpose!r}")

    ok_secs = [r["sec"] for r in recs if r["ok"]]
    price = PRICES.get(name)
    cost_per_call = None
    if price:
        total = sum(((r.get("pt", 0) - r.get("cached", 0)) * price[0] + r.get("cached", 0) * price[1]
                     + r.get("ct", 0) * price[2]) / 1e6 for r in recs)
        cost_per_call = total / len(recs)
    return {
        "model": name, "cases": len(CASES), **n,
        "avg": statistics.mean(ok_secs) if ok_secs else 0.0,
        "p90": sorted(ok_secs)[int(len(ok_secs) * 0.9) - 1] if ok_secs else 0.0,
        "max": max(ok_secs) if ok_secs else 0.0,
        "reason": statistics.mean([r.get("reason", 0) for r in recs if r["ok"]]) if ok_secs else 0.0,
        "per1k": cost_per_call * 1000 if cost_per_call is not None else None,
    }


def main() -> None:
    models = [m if m.startswith("accounts/") else MODEL_PREFIX + m for m in sys.argv[1:]] or [settings.llm_model]

    directory = fetch_cafe_directory(settings)
    allowed = sorted({t for c in directory for t in pickbot_rag._region_tokens(c["address"])})
    used = set().union(*(c[1] | c[2] for c in CASES))
    missing = used - set(allowed)
    print(f"허용 지역 {len(allowed)}개: {', '.join(allowed)}")
    if missing:
        print(f"!! 케이스의 기대 지역 중 현재 데이터에 없는 값: {sorted(missing)} — 주소 데이터가 바뀌었다면 케이스를 고치세요")

    summary = [run_model(m, directory, allowed) for m in models]

    print(f"\n{'=' * 78}\n요약 ({len(CASES)}건)\n{'=' * 78}")
    print(f"{'모델':<32} {'완전정답':>6} {'지역필터':>6} {'조건유무':>6} {'지역섞임X':>7} {'핵심어':>6} "
          f"{'실패':>4} {'평균s':>6} {'p90s':>6} {'최대s':>6} {'추론토큰':>6} {'1천건$':>7}")
    for s in summary:
        per1k = f"{s['per1k']:.2f}" if s["per1k"] is not None else "-"
        print(f"{s['model']:<32} {s['all']:>3}/{s['cases']:<2} {s['filter']:>3}/{s['cases']:<2} "
              f"{s['purpose_has']:>3}/{s['purpose_has_total']:<2} {s['clean']:>4}/{s['cases']:<2} "
              f"{s['keyword']:>3}/{s['cases']:<2} {s['fail']:>4} {s['avg']:6.1f} {s['p90']:6.1f} {s['max']:6.1f} "
              f"{s['reason']:6.0f} {per1k:>7}")


if __name__ == "__main__":
    main()
