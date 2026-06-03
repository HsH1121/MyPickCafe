import csv
import glob
import json
import os
import random
import re

import ollama

NAVER_CSV_DIR = os.path.join(os.path.dirname(__file__), 'naver')

ALLOWED_TAGS = {
    "FACILITY": {"PLUG", "TERRACE", "PET", "PARKING", "WIFI"},
    "MENU":     {"AMERICANO", "LATTE", "COLDBREW", "BAKERY", "CAKE", "ADE", "DESSERT"},
    "MOOD":     {"MODERN", "RETRO", "NATURE", "INDUSTRIAL", "CLASSIC"},
    "PURPOSE":  {"STUDY", "TALK", "REST", "DATE", "PHOTO", "MEETING"},
}


def _extract_tags(content: str, attempt: int = 0) -> dict:
    try:
        response = ollama.chat(
            model='qwen2.5:7b',
            messages=[
                {
                    'role': 'system',
                    'content': (
                        "리뷰 텍스트에서 명시적으로 언급된 내용만 태그로 추출하여 JSON으로 반환하세요.\n"
                        "부정 언급 태그는 제외. 언급되지 않은 카테고리는 반드시 빈 배열([])로 반환.\n\n"
                        "허용 태그:\n"
                        "FACILITY: PLUG, TERRACE, PET, PARKING, WIFI\n"
                        "MENU: AMERICANO, LATTE, COLDBREW, BAKERY, CAKE, ADE, DESSERT\n"
                        "MOOD: MODERN, RETRO, NATURE, INDUSTRIAL, CLASSIC\n"
                        "PURPOSE: STUDY, TALK, REST, DATE, PHOTO, MEETING\n\n"
                        'JSON 형식: {"FACILITY": [], "MENU": [], "MOOD": [], "PURPOSE": []}'
                    ),
                },
                {'role': 'user', 'content': f"리뷰: {content}"},
            ],
            format='json',
            options={'temperature': 0.2, 'num_predict': 150},
        )
        raw = json.loads(response['message']['content'])
        return {
            cat: [tag for tag in (raw.get(cat) or []) if tag in allowed]
            for cat, allowed in ALLOWED_TAGS.items()
        }
    except Exception as e:
        if attempt < 2:
            return _extract_tags(content, attempt + 1)
        print(f"  [경고] 태그 추출 실패, 빈 태그로 처리: {e}")
        return {cat: [] for cat in ALLOWED_TAGS}


def generate(member_count: int) -> list:
    csv_files = sorted(glob.glob(os.path.join(NAVER_CSV_DIR, '*.csv')))
    sql_lines = ["-- Review + ReviewTag Dummy Data", ""]
    total     = 0

    print(f"🚀 [4/4] 리뷰 더미 데이터 생성 시작")

    for csv_file in csv_files:
        filename      = os.path.basename(csv_file)
        cafe_name     = re.sub(r'^\d+_', '', filename).replace('_리뷰.csv', '')
        cafe_name_esc = cafe_name.replace("'", "''")

        try:
            with open(csv_file, 'r', encoding='utf-8') as f:
                reader = csv.reader(f)
                next(reader)  # skip "review" header
                for row in reader:
                    if not row:
                        continue
                    content = row[0].strip()
                    content = re.sub(r'\n?접기$', '', content).strip()
                    content = content.replace('\n', ' ')
                    if not content:
                        continue

                    member_email = f"user{random.randint(1, member_count):05d}@test.com"
                    content_esc  = content[:1000].replace("'", "''")

                    sql_lines.append(
                        f"INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) "
                        f"VALUES ("
                        f"(SELECT cafe_id FROM cafe WHERE name = '{cafe_name_esc}'), "
                        f"(SELECT member_id FROM member WHERE email = '{member_email}'), "
                        f"'{content_esc}', 0, 0, NULL, SYSTIMESTAMP"
                        f");"
                    )

                    tags = _extract_tags(content)
                    for category, codes in tags.items():
                        for code in codes:
                            sql_lines.append(
                                f"INSERT INTO review_tag (review_id, category_code, code) "
                                f"VALUES ((SELECT MAX(review_id) FROM review), '{category}', '{code}');"
                            )

                    sql_lines.append("")
                    total += 1
        except Exception as e:
            print(f"  [경고] {filename} 읽기 실패: {e}")

        print(f"  [{csv_files.index(csv_file) + 1:03d}/{len(csv_files)}] {cafe_name} 완료")

    print(f"  ✅ review {total}건 완료\n")
    return sql_lines
