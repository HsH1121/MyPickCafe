import json
import random
import time
import ollama

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


def _generate(cafe_name: str, rating: int, persona: str, features: list) -> dict:
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
                        f"당신은 카페를 방문한 실제 고객입니다. 리뷰를 작성하고 태그를 추출하여 JSON으로 반환하세요.\n\n"
                        f"조건 1: 말투는 [{persona}]로 작성.\n"
                        f"조건 2: 별점은 {rating}점이며 전체 뉘앙스는 [{sentiment_guide}] 느낌.\n"
                        f"조건 3: 주로 언급할 카페 특징은 [{', '.join(features)}].\n"
                        f"조건 4: 리뷰에서 명시적으로 언급된 내용만 태그로 추출. 부정 언급 태그 제외.\n\n"
                        f"허용 태그:\n"
                        f"FACILITY: PLUG, TERRACE, PET, PARKING, WIFI\n"
                        f"MENU: AMERICANO, LATTE, COLDBREW, BAKERY, CAKE, ADE, DESSERT\n"
                        f"MOOD: MODERN, RETRO, NATURE, INDUSTRIAL, CLASSIC\n"
                        f"PURPOSE: STUDY, TALK, REST, DATE, PHOTO, MEETING\n\n"
                        f"반드시 아래 JSON 형식으로만 출력:\n"
                        f'{{"content": "리뷰본문", "sentiment": "GOOD or BAD or null", '
                        f'"FACILITY": [], "MENU": [], "MOOD": [], "PURPOSE": []}}'
                    ),
                },
                {'role': 'user', 'content': f"'{cafe_name}' 카페 리뷰를 작성하고 태그를 추출해줘."},
            ],
            format='json',
            options={'temperature': 0.85, 'top_p': 0.9, 'num_predict': 120, 'seed': random.randint(1, 999999)},
        )
        raw = json.loads(response['message']['content'])

        content   = raw.get('content', '방문했는데 나쁘지 않았어요.')
        sentiment = raw.get('sentiment')
        if sentiment not in ('GOOD', 'BAD'):
            sentiment = "GOOD" if rating >= 4 else "BAD" if rating <= 2 else None

        tags = {
            cat: [tag for tag in raw.get(cat, []) if tag in allowed]
            for cat, allowed in ALLOWED_TAGS.items()
        }
        return {'content': content, 'sentiment': sentiment, 'tags': tags}

    except Exception as e:
        print(f"  [경고] 생성 실패: {e}")
        sentiment = "GOOD" if rating >= 4 else "BAD" if rating <= 2 else None
        return {'content': '방문했는데 나쁘지 않았어요.', 'sentiment': sentiment, 'tags': {cat: [] for cat in ALLOWED_TAGS}}


def generate(cafe_names: list, total_reviews: int, member_count: int) -> list:
    sql_lines = ["-- Review + ReviewTag Dummy Data", ""]
    print(f"🚀 [4/4] 리뷰 더미 데이터 생성 시작 ({total_reviews}개)")

    for i in range(1, total_reviews + 1):
        t            = time.time()
        rating       = random.choice([1, 2, 3, 4, 5])
        persona      = random.choice(REVIEWER_PERSONAS)
        features     = random.sample(CAFE_FEATURES, 2)
        cafe_name    = random.choice(cafe_names)
        member_email = f"user{random.randint(1, member_count):05d}@test.com"

        result    = _generate(cafe_name, rating, persona, features)
        content   = result['content']
        sentiment = result['sentiment']
        tags      = result['tags']
        good      = 1 if sentiment == "GOOD" else 0
        bad       = 1 if sentiment == "BAD"  else 0
        sentiment_sql = f"'{sentiment}'" if sentiment else "NULL"

        content_esc   = content[:1000].replace("'", "''")
        cafe_name_esc = cafe_name.replace("'", "''")

        sql_lines.append(
            f"INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) "
            f"VALUES ("
            f"(SELECT cafe_id FROM cafe WHERE name = '{cafe_name_esc}'), "
            f"(SELECT member_id FROM member WHERE email = '{member_email}'), "
            f"'{content_esc}', {good}, {bad}, {sentiment_sql}, SYSTIMESTAMP"
            f");"
        )

        tag_count = 0
        for category, codes in tags.items():
            for code in codes:
                sql_lines.append(
                    f"INSERT INTO review_tag (review_id, category_code, code) "
                    f"VALUES ((SELECT MAX(review_id) FROM review), '{category}', '{code}');"
                )
                tag_count += 1

        sql_lines.append("")

        tag_summary = {cat: codes for cat, codes in tags.items() if codes}
        print(f"  [{i:03d}/{total_reviews}] {cafe_name} | ★{rating} {sentiment} | 태그 {tag_count}개 ({time.time()-t:.2f}s)")
        print(f"  {content[:80]}...")
        print(f"  태그: {tag_summary}")
        print("  " + "-" * 60)
        time.sleep(0.1)

    print(f"  ✅ 리뷰 {total_reviews}개 완료\n")
    return sql_lines
