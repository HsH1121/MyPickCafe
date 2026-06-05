"""
Dummy 태그 추출 심층 테스트 (test_api.py 와 동일 케이스)
- Ollama만 실행 중이면 됩니다 (FastAPI 서버 불필요)
- 카테고리별 Precision / Recall / F1 출력
"""

import sys
sys.stdout.reconfigure(encoding='utf-8')

import json
import ollama

MODEL        = "qwen2.5:14b"
NUM_PREDICT  = 1000
TEMPERATURE  = 0.0
TOP_P        = 0.9

ALLOWED_TAGS = {
    "FACILITY": {"PLUG", "TERRACE", "PET", "PARKING", "WIFI"},
    "MENU":     {"AMERICANO", "LATTE", "COLDBREW", "BAKERY", "CAKE", "ADE", "DESSERT"},
    "MOOD":     {"MODERN", "RETRO", "NATURE", "INDUSTRIAL", "CLASSIC"},
    "PURPOSE":  {"STUDY", "TALK", "REST", "DATE", "PHOTO", "MEETING"},
}

_SYSTEM_PROMPT = """당신은 한국 카페 리뷰를 분석하는 전문 AI입니다.

## 임무
리뷰 텍스트에서 카페 특징을 4개 카테고리 태그로 분류하고 전체 감성을 JSON으로 반환합니다.

## 반환 가능한 태그 목록 (이 목록 외의 태그는 절대 사용 금지)
- FACILITY: WIFI(와이파이), PLUG(콘센트/충전), TERRACE(야외/테라스), PET(반려동물 동반), PARKING(주차)
- MENU:     AMERICANO(아메리카노), LATTE(라떼), COLDBREW(콜드브루), BAKERY(베이커리/빵), CAKE(케이크), ADE(에이드), DESSERT(디저트)
- PURPOSE:  STUDY(공부/작업), TALK(대화/수다), REST(휴식/혼자), DATE(데이트/연인), PHOTO(사진/인스타), MEETING(비즈니스/모임)
- MOOD:     MODERN(모던/미니멀), RETRO(레트로/빈티지), NATURE(자연/식물/우드), INDUSTRIAL(인더스트리얼), CLASSIC(클래식/고급스러운)

## 핵심 분석 원칙

### 1. 명시적으로 언급된 것만 태그로 추가
리뷰에서 명확히 언급된 항목만 포함하세요. 추측 금지.

### 2. 부정문 처리
부정적으로 언급된 항목은 태그에 포함하지 마세요.
- "와이파이가 느려요" → WIFI 미포함
- "콘센트가 없어요" → PLUG 미포함
- "주차 불편해요" → PARKING 미포함

### 3. 감성 판단
- 전체 톤이 명확히 긍정: "GOOD"
- 명확히 부정: "BAD"
- 중립·혼재: null

## Few-shot 예시

### 예시 1
입력: 리뷰="콘센트도 많고 와이파이도 빵빵해서 노트북 작업하기 딱 좋아요. 아메리카노도 맛있었어요."
출력: {"FACILITY": ["WIFI", "PLUG"], "MENU": ["AMERICANO"], "PURPOSE": ["STUDY"], "MOOD": [], "sentiment": "GOOD"}

### 예시 2
입력: 리뷰="인더스트리얼 인테리어가 너무 예쁘고 사진 찍기 좋아요. 에이드도 색깔이 예뻐서 맛있었어요."
출력: {"FACILITY": [], "MENU": ["ADE"], "PURPOSE": ["PHOTO"], "MOOD": ["INDUSTRIAL"], "sentiment": "GOOD"}

### 예시 3
입력: 리뷰="테라스가 있어서 강아지랑 같이 왔어요. 케이크랑 라떼 맛있었고 분위기가 레트로해서 좋았어요."
출력: {"FACILITY": ["TERRACE", "PET"], "MENU": ["CAKE", "LATTE"], "PURPOSE": [], "MOOD": ["RETRO"], "sentiment": "GOOD"}

### 예시 4
입력: 리뷰="주차도 편하고 넓어서 미팅하기 좋았어요. 빵도 맛있어요."
출력: {"FACILITY": ["PARKING"], "MENU": ["BAKERY"], "PURPOSE": ["MEETING"], "MOOD": [], "sentiment": "GOOD"}

### 예시 5
입력: 리뷰="시끄럽고 와이파이도 없어서 공부하기 너무 불편했어요. 음료 맛도 별로."
출력: {"FACILITY": [], "MENU": [], "PURPOSE": [], "MOOD": [], "sentiment": "BAD"}

### 예시 6
입력: 리뷰="모던하고 깔끔한 분위기에서 디저트 먹으면서 친구랑 수다 떨기 너무 좋아요."
출력: {"FACILITY": [], "MENU": ["DESSERT"], "PURPOSE": ["TALK"], "MOOD": ["MODERN"], "sentiment": "GOOD"}

### 예시 7
입력: 리뷰="클래식한 느낌의 카페. 콜드브루가 특히 맛있고 혼자 쉬다가기 좋아요."
출력: {"FACILITY": [], "MENU": ["COLDBREW"], "PURPOSE": ["REST"], "MOOD": ["CLASSIC"], "sentiment": "GOOD"}

### 예시 8
입력: 리뷰="연인이랑 데이트하기 딱 좋은 분위기예요. 식물이 많아서 자연스러운 느낌이에요."
출력: {"FACILITY": [], "MENU": [], "PURPOSE": ["DATE"], "MOOD": ["NATURE"], "sentiment": "GOOD"}

## 출력 규칙 (엄격 준수)
- 반드시 유효한 JSON 객체 하나만 반환하세요. 설명·부연 텍스트 절대 금지.
- FACILITY / MENU / PURPOSE / MOOD 키를 모두 포함하세요.
- 각 카테고리의 태그는 0개(빈 배열 [])일 수도 있고, 1개 또는 여러 개일 수도 있습니다. 리뷰에서 언급된 만큼만 포함하세요.
- 각 태그는 반드시 위 목록에 있는 값만 사용하세요.
- "sentiment": "GOOD" | "BAD" | null (중립·혼재일 때만 null, 그 외에는 반드시 "GOOD" 또는 "BAD")

## 배치 처리
여러 리뷰를 한 번에 처리할 때는 아래 형식으로 반환하세요:
{"results": [<리뷰1 결과>, <리뷰2 결과>, ...]}
"""

# ── 테스트 케이스 (test_api.py 와 동일 리뷰, Dummy 포맷) ─────────────────────
# (리뷰 텍스트, 기대 태그 dict[FACILITY/MENU/MOOD/PURPOSE → set])
# ※ Dummy는 sentiment 없음
TEST_CASES = [
    (
        "콘센트도 많고 와이파이도 빠르고 아메리카노가 맛있었어요. 공부하기 딱 좋은 카페!",
        {"FACILITY": {"PLUG","WIFI"}, "MENU": {"AMERICANO"}, "PURPOSE": {"STUDY"}, "MOOD": set()},
    ),
    (
        "생일 케이크 주문했어요. 너무 예쁘게 만들어주셔서 감동이었어요.",
        {"FACILITY": set(), "MENU": {"CAKE"}, "PURPOSE": set(), "MOOD": set()},
    ),
    (
        "주차 불편하고 와이파이 없어요. 커피는 나쁘지 않았어요.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
    ),
    (
        "시끄럽고 와이파이도 안 되고 음료 맛도 없어요. 다시는 안 올 것 같아요.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
    ),
    (
        "노출 콘크리트 인더스트리얼 감성이 너무 좋아요. 혼자 작업하러 자주 옵니다.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": {"STUDY"}, "MOOD": {"INDUSTRIAL"}},
    ),
    (
        "레트로 빈티지 인테리어가 너무 예쁘고 콜드브루가 맛있었어요. 친구랑 수다 떨기 좋아요.",
        {"FACILITY": set(), "MENU": {"COLDBREW"}, "PURPOSE": {"TALK"}, "MOOD": {"RETRO"}},
    ),
    (
        "식물이 많아서 자연스러운 분위기에요. 에이드 색이 예뻐서 인스타 사진 찍기 딱 좋아요.",
        {"FACILITY": set(), "MENU": {"ADE"}, "PURPOSE": {"PHOTO"}, "MOOD": {"NATURE"}},
    ),
    (
        "모던하고 깔끔한 인테리어에 디저트가 맛있어요. 혼자 쉬다가기 좋아요.",
        {"FACILITY": set(), "MENU": {"DESSERT"}, "PURPOSE": {"REST"}, "MOOD": {"MODERN"}},
    ),
    (
        "주차 편하고 넓어서 비즈니스 미팅하기 좋았어요. 빵도 맛있고 클래식한 분위기예요.",
        {"FACILITY": {"PARKING"}, "MENU": {"BAKERY"}, "PURPOSE": {"MEETING"}, "MOOD": {"CLASSIC"}},
    ),
    (
        "연인이랑 강아지 데려와서 테라스에서 커피 마셨어요. 너무 좋았어요!",
        {"FACILITY": {"TERRACE","PET"}, "MENU": set(), "PURPOSE": {"DATE"}, "MOOD": set()},
    ),
    (
        "와이파이 빠르고 라떼 진하고 모던한 인테리어가 좋아요. 친구랑 수다 떨기 좋았어요.",
        {"FACILITY": {"WIFI"}, "MENU": {"LATTE"}, "PURPOSE": {"TALK"}, "MOOD": {"MODERN"}},
    ),
    (
        "라떼는 맛있었는데 소금빵은 좀 별로였어요. 인테리어는 예뻤어요.",
        {"FACILITY": set(), "MENU": {"LATTE"}, "PURPOSE": set(), "MOOD": set()},
    ),
    (
        "친구랑 오랜만에 왔는데 역시 맛있네요 ㅎㅎ",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
    ),
    (
        "조용하고 아메리카노가 진해서 혼자 쉬기 딱이에요.",
        {"FACILITY": set(), "MENU": {"AMERICANO"}, "PURPOSE": {"REST"}, "MOOD": set()},
    ),
    (
        "완전 별로임. 비추.",
        {"FACILITY": set(), "MENU": set(), "PURPOSE": set(), "MOOD": set()},
    ),
]


def _filter(raw: dict) -> dict:
    return {
        cat: [t for t in (raw.get(cat) or []) if t in allowed]
        for cat, allowed in ALLOWED_TAGS.items()
    }


def _batch(reviews: list[str], attempt: int = 0) -> list[dict]:
    numbered = "\n".join(f"{i+1}. {r}" for i, r in enumerate(reviews))
    try:
        resp = ollama.chat(
            model=MODEL,
            messages=[
                {"role": "system", "content": _SYSTEM_PROMPT},
                {"role": "user",   "content": f"리뷰 목록:\n{numbered}\n\n결과:"},
            ],
            format="json",
            options={"temperature": TEMPERATURE, "top_p": TOP_P, "num_predict": NUM_PREDICT},
        )
        raw = json.loads(resp["message"]["content"])
        results = raw.get("results", [])
        while len(results) < len(reviews):
            results.append({})
        return [_filter(r) for r in results[:len(reviews)]]
    except Exception as e:
        if attempt < 2:
            return _batch(reviews, attempt + 1)
        print(f"  [경고] 배치 실패: {e}")
        return [{cat: [] for cat in ALLOWED_TAGS} for _ in reviews]


def _score(got_list: list, exp_set: set) -> tuple[int, int, int]:
    got = set(got_list)
    return len(exp_set & got), len(got - exp_set), len(exp_set - got)


# ── main ──────────────────────────────────────────────────────────────────────

def run() -> None:
    cats = list(ALLOWED_TAGS.keys())
    cat_tp = {c: 0 for c in cats}
    cat_fp = {c: 0 for c in cats}
    cat_fn = {c: 0 for c in cats}
    case_ok = 0

    reviews = [r for r, _ in TEST_CASES]
    BATCH = 10

    print("=" * 72)
    print(f"{'Dummy 태그 추출  심층 테스트  (15 cases)':^72}")
    print(f"{'model: ' + MODEL + '  temp=' + str(TEMPERATURE):^72}")
    print("=" * 72)

    results_all: list[dict] = []
    for i in range(0, len(reviews), BATCH):
        chunk = reviews[i:i + BATCH]
        results_all.extend(_batch(chunk))

    for idx, ((review, exp_tags), result) in enumerate(zip(TEST_CASES, results_all), 1):
        tag_ok = True
        for cat in cats:
            tp, fp, fn = _score(result.get(cat, []), exp_tags[cat])
            cat_tp[cat] += tp
            cat_fp[cat] += fp
            cat_fn[cat] += fn
            if fp or fn:
                tag_ok = False

        if tag_ok:
            case_ok += 1

        sym = "✓" if tag_ok else "✗"
        print(f"[TAG{sym}] [{idx:02d}] {review[:55]!r}")
        if not tag_ok:
            for cat in cats:
                got_s = set(result.get(cat, []))
                if got_s != exp_tags[cat]:
                    print(f"         {cat}: 기대={sorted(exp_tags[cat])}  결과={sorted(got_s)}")

    # ── 통계 ──────────────────────────────────────────────────────────────────
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
        print(f"  {cat:<10} P={prec:.2f}  R={rec:.2f}  F1={f1:.2f}  {bar}")

    prec = total_tp / (total_tp + total_fp) if (total_tp + total_fp) else 1.0
    rec  = total_tp / (total_tp + total_fn) if (total_tp + total_fn) else 1.0
    f1   = 2*prec*rec / (prec+rec) if (prec+rec) else 0.0

    print("-" * 72)
    print(f"  {'전체 태그':<10} P={prec:.2f}  R={rec:.2f}  F1={f1:.2f}")
    print(f"  전체 케이스 완전 일치: {case_ok}/{len(TEST_CASES)}")
    print("=" * 72)


if __name__ == "__main__":
    run()
