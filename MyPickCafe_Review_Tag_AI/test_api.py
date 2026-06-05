"""
FastAPI 모델 세팅 심층 테스트 — 서버/Oracle 없이 직접 호출
- ollama_client.call_ollama() + prompt_builder 직접 사용
- temperature=0.0, top_p=0.9, num_predict=1000, LLM 키: FACILITY/MENU/PURPOSE/MOOD
- test_deep.py 와 동일 15개 케이스로 성능 비교
"""

import sys
sys.path.insert(0, str(__import__("pathlib").Path(__file__).parent))
sys.stdout.reconfigure(encoding='utf-8')

import asyncio
from schemas import ReviewRequest
from ollama_client import call_ollama
from prompt_builder import SYSTEM_PROMPT, build_user_message
from prompt_builder import (
    ALLOWED_FACILITY_TAGS, ALLOWED_MENU_TAGS,
    ALLOWED_PURPOSE_TAGS, ALLOWED_MOOD_TAGS,
)
from config import Settings

settings = Settings()

# ── 테스트 케이스 (test_deep.py 와 동일 리뷰 순서) ────────────────────────────
# (리뷰 텍스트, 기대 태그, 기대 sentiment)
TEST_CASES = [
    (
        "콘센트도 많고 와이파이도 빠르고 아메리카노가 맛있었어요. 공부하기 딱 좋은 카페!",
        {"FACILITY": {"PLUG","WIFI"}, "MENU": {"AMERICANO"}, "PURPOSE": {"STUDY"}, "MOOD": set()},
        "GOOD",
    ),
    (
        "생일 케이크 주문했어요. 너무 예쁘게 만들어주셔서 감동이었어요.",
        {"FACILITY": set(), "MENU": {"CAKE"}, "PURPOSE": set(), "MOOD": set()},
        "GOOD",
    ),
    (
        "주차 불편하고 와이파이 없어요. 커피는 나쁘지 않았어요.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
        None,
    ),
    (
        "시끄럽고 와이파이도 안 되고 음료 맛도 없어요. 다시는 안 올 것 같아요.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
        "BAD",
    ),
    (
        "노출 콘크리트 인더스트리얼 감성이 너무 좋아요. 혼자 작업하러 자주 옵니다.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": {"STUDY"}, "MOOD": {"INDUSTRIAL"}},
        "GOOD",
    ),
    (
        "레트로 빈티지 인테리어가 너무 예쁘고 콜드브루가 맛있었어요. 친구랑 수다 떨기 좋아요.",
        {"FACILITY": set(), "MENU": {"COLDBREW"}, "PURPOSE": {"TALK"}, "MOOD": {"RETRO"}},
        "GOOD",
    ),
    (
        "식물이 많아서 자연스러운 분위기에요. 에이드 색이 예뻐서 인스타 사진 찍기 딱 좋아요.",
        {"FACILITY": set(), "MENU": {"ADE"}, "PURPOSE": {"PHOTO"}, "MOOD": {"NATURE"}},
        "GOOD",
    ),
    (
        "모던하고 깔끔한 인테리어에 디저트가 맛있어요. 혼자 쉬다가기 좋아요.",
        {"FACILITY": set(), "MENU": {"DESSERT"}, "PURPOSE": {"REST"}, "MOOD": {"MODERN"}},
        "GOOD",
    ),
    (
        "주차 편하고 넓어서 비즈니스 미팅하기 좋았어요. 빵도 맛있고 클래식한 분위기예요.",
        {"FACILITY": {"PARKING"}, "MENU": {"BAKERY"}, "PURPOSE": {"MEETING"}, "MOOD": {"CLASSIC"}},
        "GOOD",
    ),
    (
        "연인이랑 강아지 데려와서 테라스에서 커피 마셨어요. 너무 좋았어요!",
        {"FACILITY": {"TERRACE","PET"}, "MENU": set(), "PURPOSE": {"DATE"}, "MOOD": set()},
        "GOOD",
    ),
    (
        "와이파이 빠르고 라떼 진하고 모던한 인테리어가 좋아요. 친구랑 수다 떨기 좋았어요.",
        {"FACILITY": {"WIFI"}, "MENU": {"LATTE"}, "PURPOSE": {"TALK"}, "MOOD": {"MODERN"}},
        "GOOD",
    ),
    (
        "라떼는 맛있었는데 소금빵은 좀 별로였어요. 인테리어는 예뻤어요.",
        {"FACILITY": set(), "MENU": {"LATTE"}, "PURPOSE": set(), "MOOD": set()},
        None,
    ),
    (
        "친구랑 오랜만에 왔는데 역시 맛있네요 ㅎㅎ",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
        "GOOD",
    ),
    (
        "조용하고 아메리카노가 진해서 혼자 쉬기 딱이에요.",
        {"FACILITY": set(), "MENU": {"AMERICANO"}, "PURPOSE": {"REST"}, "MOOD": set()},
        "GOOD",
    ),
    (
        "완전 별로임. 비추.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
        "BAD",
    ),
]

ALLOWED = {
    "FACILITY": ALLOWED_FACILITY_TAGS,
    "MENU":     ALLOWED_MENU_TAGS,
    "PURPOSE":  ALLOWED_PURPOSE_TAGS,
    "MOOD":     ALLOWED_MOOD_TAGS,
}
VALID_SENTIMENTS = frozenset({"GOOD", "BAD"})


def _extract(raw: dict) -> dict:
    return {
        "FACILITY": [t for t in (raw.get("FACILITY") or []) if t in ALLOWED_FACILITY_TAGS],
        "MENU":     [t for t in (raw.get("MENU")     or []) if t in ALLOWED_MENU_TAGS],
        "PURPOSE":  [t for t in (raw.get("PURPOSE")  or []) if t in ALLOWED_PURPOSE_TAGS],
        "MOOD":     [t for t in (raw.get("MOOD")     or []) if t in ALLOWED_MOOD_TAGS],
        "sentiment":    raw.get("sentiment") if raw.get("sentiment") in VALID_SENTIMENTS else None,
    }


def _score(got_list: list, exp_set: set) -> tuple[int, int, int]:
    got = set(got_list)
    return len(exp_set & got), len(got - exp_set), len(exp_set - got)


async def run() -> None:
    cats = ["FACILITY", "MENU", "PURPOSE", "MOOD"]
    cat_tp = {c: 0 for c in cats}
    cat_fp = {c: 0 for c in cats}
    cat_fn = {c: 0 for c in cats}
    sent_ok = sent_total = case_ok = 0

    print("=" * 72)
    print(f"{'FastAPI 모델 세팅  심층 테스트  (15 cases)':^72}")
    print(f"{'model=' + settings.ollama_model + '  temp=0.0  top_p=0.9  num_predict=1000':^72}")
    print("=" * 72)

    for idx, (review, exp_tags, exp_sent) in enumerate(TEST_CASES, 1):
        req = ReviewRequest(reviewId=idx, reviewText=review)
        try:
            raw = await call_ollama(
                system_prompt=SYSTEM_PROMPT,
                user_message=build_user_message(req),
                model=settings.ollama_model,
                base_url=settings.model_api_url,
                timeout=settings.ollama_timeout,
            )
            res = _extract(raw)
        except Exception as e:
            print(f"[ERR] [{idx:02d}] {e}")
            continue

        tag_ok = True
        for cat in cats:
            tp, fp, fn = _score(res[cat], exp_tags[cat])
            cat_tp[cat] += tp
            cat_fp[cat] += fp
            cat_fn[cat] += fn
            if fp or fn:
                tag_ok = False

        got_sent = res["sentiment"]
        sent_match = (got_sent == exp_sent)
        sent_total += 1
        if sent_match:
            sent_ok += 1

        if tag_ok and sent_match:
            case_ok += 1

        t = "✓" if tag_ok   else "✗"
        s = "✓" if sent_match else "✗"
        print(f"[TAG{t} SENT{s}] [{idx:02d}] {review[:52]!r}")
        if not tag_ok:
            for cat in cats:
                got_s = set(res[cat])
                if got_s != exp_tags[cat]:
                    print(f"         {cat}: 기대={sorted(exp_tags[cat])}  결과={sorted(got_s)}")
        if not sent_match:
            print(f"         sentiment: 기대={exp_sent!r}  결과={got_sent!r}")

    print("\n" + "=" * 72)
    print(f"{'카테고리별 Precision / Recall / F1':^72}")
    print("-" * 72)
    total_tp = total_fp = total_fn = 0
    for cat in cats:
        tp, fp, fn = cat_tp[cat], cat_fp[cat], cat_fn[cat]
        total_tp += tp; total_fp += fp; total_fn += fn
        prec = tp / (tp + fp) if (tp + fp) else 1.0
        rec  = tp / (tp + fn) if (tp + fn) else 1.0
        f1   = 2*prec*rec / (prec+rec) if (prec+rec) else 0.0
        bar  = "█" * round(f1 * 20)
        print(f"  {cat:<15} P={prec:.2f}  R={rec:.2f}  F1={f1:.2f}  {bar}")

    prec = total_tp / (total_tp + total_fp) if (total_tp + total_fp) else 1.0
    rec  = total_tp / (total_tp + total_fn) if (total_tp + total_fn) else 1.0
    f1   = 2*prec*rec / (prec+rec) if (prec+rec) else 0.0
    sent_acc = sent_ok / sent_total if sent_total else 0.0

    print("-" * 72)
    print(f"  {'전체 태그':<15} P={prec:.2f}  R={rec:.2f}  F1={f1:.2f}")
    print(f"  {'감성(GOOD/BAD)':<15} {sent_ok}/{sent_total} = {sent_acc:.1%}")
    print(f"  {'전체 케이스':<15} {case_ok}/{len(TEST_CASES)} 완전 일치")
    print("=" * 72)


if __name__ == "__main__":
    asyncio.run(run())
