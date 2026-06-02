import json
import os
import random
import time
import ollama

# -----------------------------------------------
# 옵션: 생성할 리뷰 수 / 리뷰 작성자 수 (create_user_dummy.py 의 MEMBER_COUNT 와 맞출 것)
TOTAL_REVIEWS = 100
MEMBER_COUNT  = 10000
# -----------------------------------------------

ALLOWED_TAGS = {
    "FACILITY": {"PLUG", "TERRACE", "PET", "PARKING", "WIFI"},
    "MENU":     {"AMERICANO", "LATTE", "COLDBREW", "BAKERY", "CAKE", "ADE", "DESSERT"},
    "MOOD":     {"MODERN", "RETRO", "NATURE", "INDUSTRIAL", "CLASSIC"},
    "PURPOSE":  {"STUDY", "TALK", "REST", "DATE", "PHOTO", "MEETING"},
}

REVIEWER_PERSONAS = [
    "20대 대학생의 트렌디하고 가벼운 말투 (~함, ~음, 대박 등 사용)",
    "30대 직장인의 정중하고 이성적인 솔직한 리뷰 (~합니다 체)",
    "기계에 익숙하지 않은 부모님 세대의 친근한 말투 (~하네요, 좋네요)",
    "이모티콘을 자주 쓰고 감성적인 말투 (ㅠㅠ, ㅎㅎ, 👍 활용)",
    "장단점을 명확하게 요약하는 까칠하고 이성적인 말투",
]

CAFE_FEATURES = [
    "와이파이 속도와 콘센트",
    "좌석 수와 공간 여유",
    "아메리카노와 라떼 맛",
    "케이크와 디저트",
    "카페 인테리어와 분위기",
    "테라스와 야외 공간",
    "주차 편의성",
    "공부와 작업하기 좋은 환경",
    "데이트하기 좋은 분위기",
    "사진 찍기 좋은 인테리어",
    "콜드브루와 에이드",
    "반려동물 동반 가능 여부",
]

# create_cafe_dummy.py 실행 후 생성된 카페명 목록 로드
if os.path.exists("_cafe_names.txt"):
    with open("_cafe_names.txt", encoding="utf-8") as f:
        cafe_names = [line.strip() for line in f if line.strip()]
    print(f"  ✅ _cafe_names.txt 로드 완료 ({len(cafe_names)}개)")
else:
    cafe_names = [f"테스트카페{i}" for i in range(1, 21)]
    print("  ⚠️  _cafe_names.txt 없음 — 기본 카페명 사용 (create_cafe_dummy.py 먼저 실행 권장)")


def generate_review_content(cafe_name: str, rating: int, persona: str, features: list) -> str:
    sentiment_guide = (
        "매우 만족스럽고 칭찬하는" if rating >= 4 else
        "아쉽거나 불만족스러운"    if rating <= 2 else
        "평범하고 무난한"
    )
    try:
        response = ollama.chat(
            model='qwen2.5:7b',
            messages=[
                {
                    'role': 'system',
                    'content': (
                        f"당신은 카페 방문 후 솔직한 리뷰를 작성하는 실제 고객입니다.\n"
                        f"조건 1: 말투는 [{persona}]로 작성.\n"
                        f"조건 2: 별점은 {rating}점이며 전체 뉘앙스는 [{sentiment_guide}] 느낌.\n"
                        f"조건 3: 주로 언급할 카페 특징은 [{', '.join(features)}].\n"
                        f"주의: AI가 쓴 것처럼 정형화된 서론('안녕하세요', '이 카페는~')이나 결론 없이 "
                        f"실제 사람이 쓴 구어체로 본문만 출력."
                    ),
                },
                {'role': 'user', 'content': f"'{cafe_name}' 카페 리뷰를 작성해줘."},
            ],
            options={'temperature': 0.85, 'top_p': 0.9, 'num_predict': 150, 'seed': random.randint(1, 999999)},
        )
        return response['message']['content'].strip()
    except Exception as e:
        print(f"  [경고] 리뷰 생성 실패: {e}")
        return "방문했는데 나쁘지 않았어요."


def analyze_tags(content: str) -> dict[str, list[str]]:
    try:
        response = ollama.chat(
            model='qwen2.5:7b',
            messages=[
                {
                    'role': 'system',
                    'content': (
                        "당신은 카페 리뷰를 분석해 태그를 추출하는 AI입니다.\n"
                        "리뷰에서 명시적으로 언급된 내용만 태그로 추출하세요. 추측 금지.\n"
                        "부정적으로 언급된 항목은 포함하지 마세요.\n\n"
                        "허용 태그 목록:\n"
                        "- FACILITY: PLUG(콘센트), TERRACE(테라스), PET(반려동물), PARKING(주차), WIFI(와이파이)\n"
                        "- MENU: AMERICANO(아메리카노), LATTE(라떼), COLDBREW(콜드브루), BAKERY(베이커리), CAKE(케이크), ADE(에이드), DESSERT(디저트)\n"
                        "- MOOD: MODERN(모던), RETRO(레트로), NATURE(자연), INDUSTRIAL(인더스트리얼), CLASSIC(클래식)\n"
                        "- PURPOSE: STUDY(공부), TALK(대화), REST(휴식), DATE(데이트), PHOTO(사진), MEETING(미팅)\n\n"
                        "반드시 아래 JSON 형식으로만 출력하세요:\n"
                        '{"FACILITY": [], "MENU": [], "MOOD": [], "PURPOSE": []}'
                    ),
                },
                {'role': 'user', 'content': f"다음 리뷰를 분석해줘:\n{content}"},
            ],
            format='json',
            options={'temperature': 0.1, 'top_p': 0.9, 'num_predict': 200, 'seed': random.randint(1, 999999)},
        )
        raw = json.loads(response['message']['content'])
        # 허용 목록 외 태그 필터링
        return {
            cat: [tag for tag in raw.get(cat, []) if tag in allowed]
            for cat, allowed in ALLOWED_TAGS.items()
        }
    except Exception as e:
        print(f"  [경고] 태그 분석 실패: {e}")
        return {cat: [] for cat in ALLOWED_TAGS}


sql_lines = ["-- Review + ReviewTag Dummy Data", ""]

print(f"\n🚀 리뷰 더미 데이터 생성 시작 ({TOTAL_REVIEWS}개)")

for i in range(1, TOTAL_REVIEWS + 1):
    t        = time.time()
    rating   = random.choice([1, 2, 3, 4, 5])
    persona  = random.choice(REVIEWER_PERSONAS)
    features = random.sample(CAFE_FEATURES, 2)
    cafe_name    = random.choice(cafe_names)
    member_email = f"user{random.randint(1, MEMBER_COUNT):05d}@test.com"
    content   = generate_review_content(cafe_name, rating, persona, features)
    sentiment = "GOOD" if rating >= 4 else "BAD" if rating <= 2 else random.choice(["GOOD", "BAD"])
    good      = 1 if sentiment == "GOOD" else 0
    bad       = 1 if sentiment == "BAD"  else 0
    tags      = analyze_tags(content)

    content_esc   = content.replace("'", "''")
    cafe_name_esc = cafe_name.replace("'", "''")

    # review INSERT
    sql_lines.append(
        f"INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) "
        f"VALUES ("
        f"(SELECT cafe_id FROM cafe WHERE name = '{cafe_name_esc}'), "
        f"(SELECT member_id FROM member WHERE email = '{member_email}'), "
        f"'{content_esc}', {good}, {bad}, '{sentiment}', SYSTIMESTAMP"
        f");"
    )

    # review_tag INSERT
    tag_count = 0
    for category, codes in tags.items():
        for code in codes:
            sql_lines.append(
                f"INSERT INTO review_tag (review_id, category_code, code) "
                f"VALUES ((SELECT MAX(review_id) FROM review), '{category}', '{code}');"
            )
            tag_count += 1

    sql_lines.append("")

    elapsed = time.time() - t
    tag_summary = {cat: codes for cat, codes in tags.items() if codes}
    print(f"  [{i:03d}/{TOTAL_REVIEWS}] {cafe_name} | ★{rating} {sentiment} | 태그 {tag_count}개 ({elapsed:.2f}s)")
    print(f"  {content[:80]}...")
    print(f"  태그: {tag_summary}")
    print("  " + "-" * 60)
    time.sleep(0.1)

with open("review_dummy.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql_lines))

print(f"\n🎉 review_dummy.sql 저장 완료! (총 {TOTAL_REVIEWS}개)")
