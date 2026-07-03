import asyncio
import csv
import glob
import os
import random
import re
import sys

# Review_Tag_AI 공유 모듈 로드
_REVIEW_TAG_AI_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), '..', '..', 'Review_Tag_AI')
)
sys.path.insert(0, _REVIEW_TAG_AI_DIR)

from config import Settings
from ollama_client import call_ollama
from prompt_builder import (
    SYSTEM_PROMPT,
    ALLOWED_FACILITY_TAGS,
    ALLOWED_MENU_TAGS,
    ALLOWED_PURPOSE_TAGS,
    ALLOWED_MOOD_TAGS,
)

NAVER_CSV_DIR = os.path.join(os.path.dirname(__file__), 'naver')
BATCH_SIZE    = 10
MIN_LENGTH    = 30

ALLOWED_TAGS = {
    "FACILITY": ALLOWED_FACILITY_TAGS,
    "MENU":     ALLOWED_MENU_TAGS,
    "PURPOSE":  ALLOWED_PURPOSE_TAGS,
    "MOOD":     ALLOWED_MOOD_TAGS,
}

_settings         = Settings()
_VALID_SENTIMENTS = frozenset({"GOOD", "BAD"})


def _extract_tags_batch(contents: list[str], attempt: int = 0) -> list[dict]:
    numbered     = "\n".join(f"{i+1}. {c}" for i, c in enumerate(contents))
    user_message = f"리뷰 목록:\n{numbered}\n\n각 리뷰를 분석하여 JSON으로 반환하세요."

    try:
        raw     = asyncio.run(call_ollama(
            system_prompt=SYSTEM_PROMPT,
            user_message=user_message,
            model=_settings.ollama_model,
            base_url=_settings.model_api_url,
            timeout=_settings.ollama_timeout,
        ))
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
    csv_files    = sorted(glob.glob(os.path.join(NAVER_CSV_DIR, '*.csv')))
    total_reviews = 0
    cafe_count   = 0
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
            cafe_count    += 1
        except Exception:
            pass
    return total_reviews, cafe_count


def generate_for_cafe(
    csv_file: str,
    cafe_name: str,
    member_count: int,
    min_length: int = MIN_LENGTH,
) -> list[str]:
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
            _r = random.random()
            if _r < 0.70:
                good_val = random.randint(0, 50)
                bad_val  = random.randint(0, 10)
            elif _r < 0.90:
                good_val = random.randint(51, 90)
                bad_val  = random.randint(11, 18)
            elif _r < 0.98:
                good_val = random.randint(91, 130)
                bad_val  = random.randint(19, 26)
            else:
                good_val = random.randint(131, 500)
                bad_val  = random.randint(27, 100)

            sql_lines.append(
                f"INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) "
                f"VALUES ("
                f"(SELECT cafe_id FROM cafe WHERE name = '{cafe_name_esc}'), "
                f"(SELECT member_id FROM member WHERE email = '{member_email}'), "
                f"'{content_esc}', {good_val}, {bad_val}, {sentiment_val}, CURRENT_TIMESTAMP"
                f");"
            )
            for category, codes in tags.items():
                if category not in ALLOWED_TAGS or not codes:
                    continue
                for code in codes:
                    sql_lines.append(
                        f"INSERT INTO review_tag (review_id, category_code, code) "
                        f"VALUES ((SELECT MAX(review_id) FROM review), '{category}', '{code}');"
                    )
            sql_lines.append("")

    return sql_lines
