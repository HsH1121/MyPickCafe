import json
import random
import time
import ollama

# 1. 생성할 상품 정보 정의
PRODUCT_NAME = "무선 노이즈 캔슬링 헤드폰"
TOTAL_REVIEWS_TO_GENERATE = 20  # 생성할 리뷰 개수

# 2. 무작위성을 높이기 위한 페르소나 및 조건 풀(Pool)
STAR_RATINGS = [1, 2, 3, 4, 5]
TONES = [
    "20대 대학생의 트렌디하고 가벼운 말투 (~함, ~음, 대박 등 사용)",
    "30대 직장인의 정중하고 이성적인 솔직한 리뷰 (~합니다 체 사용)",
    "기계에 익숙하지 않은 부모님 세대의 친근한 말투 (~하네요, 좋네요 체 사용)",
    "이모티콘을 자주 쓰고 감성적인 말투 (ㅠㅠ, ㅎㅎ, 👍 등 활용)",
    "장단점을 명확하게 요약해서 적는 까칠하고 이성적인 말투"
]
KEY_FEATURES = ["음질", "노이즈 캔슬링 성능", "착용감", "배터리 수명", "디자인", "배송 상태"]


def generate_single_review(product, review_id):
    # 매 반복마다 다른 조건 조합 생성 (시드 다양화)
    rating = random.choice(STAR_RATINGS)
    tone = random.choice(TONES)
    # 특징 중 2가지 무작위 선택
    features = random.sample(KEY_FEATURES, 2)

    # 긍정/부정 가이드라인 설정
    sentiment = "매우 만족스럽고 칭찬하는" if rating >= 4 else "아쉽거나 불만족스러운" if rating <= 2 else "평범하고 무난한"

    # 시스템 프롬프트를 매번 다르게 주입하여 세션 초기화 효과 극대화
    system_prompt = (
        f"당신은 쇼핑몰에 상품 리뷰를 작성하는 실제 구매자입니다.\n"
        f"다음 조건에 맞춰서 극도로 자연스럽고 리얼한 리뷰를 '딱 한 건'만 작성하세요.\n"
        f"조건 1: 말투는 [{tone}]로 작성할 것.\n"
        f"조건 2: 별점은 {rating}점이며, 전체적인 뉘앙스는 [{sentiment}] 느낌이어야 합니다.\n"
        f"조건 3: 주로 언급할 제품의 특징은 [{', '.join(features)}] 입니다.\n"
        f"주의: AI가 쓴 것처럼 정형화된 서론('안녕하세요', '이 제품은~')이나 결론은 절대 생략하고, 실제 사람이 쓴 것처럼 구어체로 작성하세요. 본문만 출력하세요."
    )

    user_prompt = f"'{product}'에 대한 리뷰를 작성해줘."

    try:
        # ollama.chat을 호출할 때 이전 대화(messages)를 누적하지 않고
        # 매번 새롭게 생성하여 호출하므로 세션이 완벽히 분리됩니다.
        response = ollama.chat(
            model='qwen2.5:7b',
            messages=[
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt}
            ],
            options={
                'temperature': 0.85,  # 값을 높여서(0.8 ~ 1.0) 매번 기상천외하고 다양한 문장이 나오도록 유도
                'top_p': 0.9,
                'num_predict': 150  # 리뷰 글자 수 제한 (토큰 낭비 방지)
            }
        )

        review_text = response['message']['content'].strip()

        return {
            "id": review_id,
            "product": product,
            "rating": rating,
            "review": review_text
        }

    except Exception as e:
        print(f"Error generating review {review_id}: {e}")
        return None


# 3. 반복문 실행 및 저장
generated_data = []

print(f"🚀 {PRODUCT_NAME}에 대한 리뷰 {TOTAL_REVIEWS_TO_GENERATE}개 생성을 시작합니다...")

for i in range(1, TOTAL_REVIEWS_TO_GENERATE + 1):
    start_time = time.time()
    review_data = generate_single_review(PRODUCT_NAME, i)

    if review_data:
        generated_data.append(review_data)
        elapsed = time.time() - start_time
        print(f"[{i}/{TOTAL_REVIEWS_TO_GENERATE}] 생성 완료 ({elapsed:.2f}초) | 별점: {review_data['rating']}점")
        print(f"리뷰: {review_data['review']}\n---------------------------------------------")

    # 로컬 GPU/CPU 과열 방지 및 안전성을 위해 아주 짧은 휴식
    time.sleep(0.1)

# 4. 결과를 JSON 파일로 저장
with open("reviews_dummy_data.json", "w", encoding="utf-8") as f:
    json.dump(generated_data, f, ensure_ascii=False, indent=4)

print("🎉 모든 더미 데이터가 'reviews_dummy_data.json' 파일로 성공적으로 저장되었습니다!")