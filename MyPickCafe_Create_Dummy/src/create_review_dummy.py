import csv
import glob
import json
import os
import random
import re

import ollama

NAVER_CSV_DIR = os.path.join(os.path.dirname(__file__), 'naver')
BATCH_SIZE    = 10
MIN_LENGTH    = 30

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


_VALID_SENTIMENTS = {"GOOD", "BAD"}


def _extract_tags_batch(contents: list[str], attempt: int = 0) -> list[dict]:
    try:
        numbered = "\n".join(f"{i+1}. {c}" for i, c in enumerate(contents))
        response = ollama.chat(
            model='qwen2.5:14b',
            messages=[
                {'role': 'system', 'content': _SYSTEM_PROMPT},
                {'role': 'user', 'content': f"리뷰 목록:\n{numbered}\n\n결과:"},
            ],
            format='json',
            options={'temperature': 0.0, 'top_p': 0.9, 'num_predict': 1000},
        )
        raw = json.loads(response['message']['content'])
        results = raw.get('results', [])
        while len(results) < len(contents):
            results.append({})
        return [
            {
                **{cat: [tag for tag in (r.get(cat) or []) if tag in allowed]
                   for cat, allowed in ALLOWED_TAGS.items()},
                'sentiment': r.get('sentiment') if r.get('sentiment') in _VALID_SENTIMENTS else None,
            }
            for r in results[:len(contents)]
        ]
    except Exception as e:
        if attempt < 2:
            return _extract_tags_batch(contents, attempt + 1)
        print(f"  [경고] 배치 태그 추출 실패, 빈 태그로 처리: {e}")
        return [{**{cat: [] for cat in ALLOWED_TAGS}, 'sentiment': None} for _ in contents]


def count_reviews(min_length: int = MIN_LENGTH) -> tuple[int, int]:
    """min_length 이상인 리뷰 수와 대상 카페 수를 반환합니다."""
    csv_files = sorted(glob.glob(os.path.join(NAVER_CSV_DIR, '*.csv')))
    total_reviews = 0
    cafe_count = 0
    for csv_file in csv_files:
        try:
            file_count = 0
            with open(csv_file, 'r', encoding='utf-8') as f:
                reader = csv.reader(f)
                next(reader)
                for row in reader:
                    if not row:
                        continue
                    content = row[0].strip()
                    content = re.sub(r'\n?접기$', '', content).strip()
                    if len(content) >= min_length:
                        file_count += 1
            total_reviews += file_count
            cafe_count += 1
        except Exception:
            pass
    return total_reviews, cafe_count


def generate_for_cafe(
    csv_file: str,
    cafe_name: str,
    member_count: int,
    min_length: int = MIN_LENGTH,
) -> list[str]:
    """단일 카페 CSV에서 리뷰 INSERT SQL 목록 반환"""
    cafe_name_esc = cafe_name.replace("'", "''")
    sql_lines: list[str] = []

    reviews = []
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        next(reader)
        for row in reader:
            if not row:
                continue
            content = row[0].strip()
            content = re.sub(r'\n?접기$', '', content).strip()
            content = content.replace('\n', ' ')
            if len(content) >= min_length:
                reviews.append(content)

    for i in range(0, len(reviews), BATCH_SIZE):
        batch     = reviews[i:i + BATCH_SIZE]
        tags_list = _extract_tags_batch([c[:500] for c in batch])

        for content, tags in zip(batch, tags_list):
            member_email  = f"user{random.randint(1, member_count):05d}@test.com"
            content_esc   = content[:1000].replace("'", "''")
            sentiment     = tags.get('sentiment')
            sentiment_val = f"'{sentiment}'" if sentiment else "NULL"
            good_val      = 1 if sentiment == 'GOOD' else 0
            bad_val       = 1 if sentiment == 'BAD'  else 0

            sql_lines.append(
                f'INSERT INTO "review" (cafe_id, member_id, content, good, bad, sentiment, created_at) '
                f'VALUES ('
                f'(SELECT cafe_id FROM "cafe" WHERE name = \'{cafe_name_esc}\'), '
                f'(SELECT member_id FROM "member" WHERE email = \'{member_email}\'), '
                f'\'{content_esc}\', {good_val}, {bad_val}, {sentiment_val}, SYSTIMESTAMP'
                f');'
            )
            for category, codes in tags.items():
                if category not in ALLOWED_TAGS or not codes:
                    continue
                for code in codes:
                    sql_lines.append(
                        f'INSERT INTO "review_tag" (review_id, category_code, code) '
                        f'VALUES ((SELECT MAX(review_id) FROM "review"), \'{category}\', \'{code}\');'
                    )
            sql_lines.append("")

    return sql_lines
