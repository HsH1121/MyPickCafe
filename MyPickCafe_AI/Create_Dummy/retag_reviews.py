"""
review_dummy.sql 의 리뷰마다 로컬 Ollama qwen2.5:14b 로 태그를 다시 계산해 review_tag INSERT 문을 만든다.

- 리뷰는 이미 DB 에 있으므로 review 는 넣지 않고, 카페 이름 + 리뷰 본문으로 review_id 를 찾아 태그만 넣는다.
- 이미 있는 태그는 ON CONFLICT DO NOTHING 으로 건너뛴다.
- LLM 은 local_llm 을 거쳐 로컬 Ollama qwen 으로만 부른다(.env 무관, 배포 환경이면 시작 시 중단).
- 중단 후 다시 실행하면 이어서 계산한다(진행 상황: retag_checkpoint.txt).

사용법 (MyPickCafe_AI/ 에서):
  python Create_Dummy/retag_reviews.py              # 전체
  python Create_Dummy/retag_reviews.py --limit 20   # 앞 20건만 (시험용)
"""
import argparse
import asyncio
import os
import re
import sys
import time

_HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.abspath(os.path.join(_HERE, '..', 'Review_Tag_AI')))

from local_llm import MODEL, OLLAMA_BASE_URL, call_qwen, ensure_local_qwen
from schemas import ReviewRequest
from prompt_builder import (
    SYSTEM_PROMPT,
    build_user_message,
    ALLOWED_FACILITY_TAGS,
    ALLOWED_MENU_TAGS,
    ALLOWED_PURPOSE_TAGS,
    ALLOWED_MOOD_TAGS,
)

CONCURRENCY = 4

SRC_SQL    = os.path.join(_HERE, "review_dummy.sql")
OUT_SQL    = os.path.join(_HERE, "review_tag_qwen.sql")
CHECKPOINT = os.path.join(_HERE, "retag_checkpoint.txt")  # 처리 완료한 리뷰 수

ALLOWED_TAGS = {
    "FACILITY": ALLOWED_FACILITY_TAGS,
    "MENU":     ALLOWED_MENU_TAGS,
    "PURPOSE":  ALLOWED_PURPOSE_TAGS,
    "MOOD":     ALLOWED_MOOD_TAGS,
}

# 이름·본문은 SQL 이스케이프('') 된 그대로 잡아 출력에 다시 쓴다.
_REVIEW_RE = re.compile(
    r"^INSERT INTO review \(cafe_id, member_id, content, good, bad, sentiment, created_at\) VALUES \("
    r"\(SELECT cafe_id FROM cafe WHERE name = '((?:[^']|'')*)'\), "
    r"\(SELECT member_id FROM member WHERE email = '[^']*'\), "
    r"'((?:[^']|'')*)', "
)


def load_reviews() -> list[tuple[str, str]]:
    """(카페 이름, 본문) — 둘 다 SQL 이스케이프된 상태."""
    reviews = []
    with open(SRC_SQL, encoding="utf-8") as f:
        for line in f:
            if line.startswith("INSERT INTO review "):
                m = _REVIEW_RE.match(line)
                if not m:
                    raise ValueError(f"형식이 다른 줄: {line[:120]}")
                reviews.append((m.group(1), m.group(2)))
    return reviews


async def extract_tags(content: str, sem: asyncio.Semaphore) -> dict[str, list[str]] | None:
    """서비스(/review/analyze)와 같은 프롬프트로 리뷰 1건 분석. 실패하면 None."""
    async with sem:
        try:
            raw = await call_qwen(
                system_prompt=SYSTEM_PROMPT,
                user_message=build_user_message(ReviewRequest(reviewId=0, reviewText=content)),
            )
        except Exception as e:
            print(f"  [경고] 태그 추출 실패: {e}")
            return None
    return {cat: [t for t in (raw.get(cat) or []) if isinstance(t, str) and t in allowed]
            for cat, allowed in ALLOWED_TAGS.items()}


def tag_sql(cafe_name: str, content: str, category: str, code: str) -> str:
    return (
        "INSERT INTO review_tag (review_id, category_code, code) "
        f"SELECT r.review_id, '{category}', '{code}' "
        "FROM review r JOIN cafe c ON c.cafe_id = r.cafe_id "
        f"WHERE c.name = '{cafe_name}' AND r.content = '{content}' "
        "ON CONFLICT (review_id, category_code, code) DO NOTHING;"
    )


async def main(limit: int | None) -> None:
    ensure_local_qwen()
    reviews = load_reviews()
    if limit:
        reviews = reviews[:limit]

    done = 0
    if os.path.exists(CHECKPOINT):
        with open(CHECKPOINT, encoding="utf-8") as f:
            done = int(f.read().strip() or 0)
    if done == 0:
        with open(OUT_SQL, "w", encoding="utf-8") as f:
            f.write(f"-- review_tag 재계산 ({MODEL}, Ollama). 리뷰는 이미 DB 에 있다고 가정한다.\n\n")

    print(f"모델: {MODEL} @ {OLLAMA_BASE_URL}")
    print(f"리뷰 {len(reviews):,}건 중 {done:,}건 완료, 남은 {len(reviews) - done:,}건\n")

    sem    = asyncio.Semaphore(CONCURRENCY)
    start  = time.time()
    first  = done
    failed = 0
    step   = CONCURRENCY * 5
    for i in range(done, len(reviews), step):
        chunk   = reviews[i:i + step]
        results = await asyncio.gather(*(
            extract_tags(content.replace("''", "'"), sem) for _, content in chunk
        ))
        lines = []
        for (cafe_name, content), tags in zip(chunk, results):
            if tags is None:
                failed += 1
                lines.append(f"-- [실패] {cafe_name}: {content[:40]}")
                continue
            for category, codes in tags.items():
                for code in codes:
                    lines.append(tag_sql(cafe_name, content, category, code))
        with open(OUT_SQL, "a", encoding="utf-8") as f:
            if lines:
                f.write("\n".join(lines) + "\n")
        done = i + len(chunk)
        with open(CHECKPOINT, "w", encoding="utf-8") as f:
            f.write(str(done))

        elapsed = time.time() - start
        eta     = elapsed / (done - first) * (len(reviews) - done)
        print(f"\r  {done:,}/{len(reviews):,}  실패 {failed}  경과 {elapsed/60:.1f}분  남은 약 {eta/60:.0f}분",
              end="", flush=True)

    print(f"\n\n완료: {OUT_SQL}")
    if failed:
        print(f"실패 {failed}건은 출력 파일에 '-- [실패]' 주석으로 남았습니다.")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--limit", type=int, help="앞에서부터 N건만 처리 (시험용)")
    asyncio.run(main(p.parse_args().limit))
