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
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

# LLM 은 local_llm 을 거쳐 로컬 Ollama qwen 으로만 부른다(.env 의 LLM_* 는 쓰지 않는다).
from local_llm import call_qwen
from schemas import ReviewRequest
from prompt_builder import (
    SYSTEM_PROMPT,
    build_user_message,
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

_VALID_SENTIMENTS = frozenset({"GOOD", "BAD"})


async def _extract_tags(content: str) -> dict:
    # 서비스(/review/analyze)와 같은 프롬프트·메시지로 리뷰 1건씩 분석한다.
    # SYSTEM_PROMPT 가 리뷰 1건 → 객체 1개 형식이라, 여러 건을 묶어 보내면 결과가 합쳐진다.
    # 재시도는 call_llm 내부에서 한다.
    try:
        raw = await call_qwen(
            system_prompt=SYSTEM_PROMPT,
            user_message=build_user_message(ReviewRequest(reviewId=0, reviewText=content)),
        )
    except Exception as e:
        print(f"  [경고] 태그 추출 실패, 빈 태그로 처리: {e}")
        return {**{cat: [] for cat in ALLOWED_TAGS}, 'sentiment': None}
    return {
        **{cat: [tag for tag in (raw.get(cat) or []) if tag in allowed]
           for cat, allowed in ALLOWED_TAGS.items()},
        'sentiment': raw.get('sentiment') if raw.get('sentiment') in _VALID_SENTIMENTS else None,
    }


def _extract_tags_batch(contents: list[str]) -> list[dict]:
    async def _run() -> list[dict]:
        return list(await asyncio.gather(*(_extract_tags(c) for c in contents)))
    return asyncio.run(_run())


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
