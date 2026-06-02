import os
import random
import time
import ollama

# -----------------------------------------------
# 옵션: 생성할 리뷰 수 / 리뷰 작성자 수 (create_member_dummy.py 의 MEMBER_COUNT 와 맞출 것)
TOTAL_REVIEWS = 100
MEMBER_COUNT  = 30
# -----------------------------------------------

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
            options={'temperature': 0.85, 'top_p': 0.9, 'num_predict': 150},
        )
        return response['message']['content'].strip()
    except Exception as e:
        print(f"  [경고] 리뷰 생성 실패: {e}")
        return "방문했는데 나쁘지 않았어요."


sql_lines = ["-- Review Dummy Data", ""]

print(f"\n🚀 리뷰 더미 데이터 생성 시작 ({TOTAL_REVIEWS}개)")

for i in range(1, TOTAL_REVIEWS + 1):
    t        = time.time()
    rating   = random.choice([1, 2, 3, 4, 5])
    persona  = random.choice(REVIEWER_PERSONAS)
    features = random.sample(CAFE_FEATURES, 2)
    cafe_name    = random.choice(cafe_names)
    member_email = f"user{random.randint(1, MEMBER_COUNT):03d}@test.com"

    content   = generate_review_content(cafe_name, rating, persona, features)
    sentiment = "GOOD" if rating >= 4 else "BAD" if rating <= 2 else random.choice(["GOOD", "BAD"])
    good      = 1 if sentiment == "GOOD" else 0
    bad       = 1 if sentiment == "BAD"  else 0

    content_esc  = content.replace("'", "''")
    cafe_name_esc = cafe_name.replace("'", "''")
    sql_lines.append(
        f"INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) "
        f"VALUES ("
        f"(SELECT cafe_id FROM cafe WHERE name = '{cafe_name_esc}'), "
        f"(SELECT member_id FROM member WHERE email = '{member_email}'), "
        f"'{content_esc}', {good}, {bad}, '{sentiment}', SYSTIMESTAMP"
        f");"
    )
    print(f"  [{i:03d}/{TOTAL_REVIEWS}] {cafe_name} | ★{rating} {sentiment} ({time.time()-t:.2f}s)")
    print(f"  {content[:80]}...")
    print("  " + "-" * 60)
    time.sleep(0.1)

with open("review_dummy.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql_lines))

print(f"\n🎉 review_dummy.sql 저장 완료! (총 {TOTAL_REVIEWS}개)")
