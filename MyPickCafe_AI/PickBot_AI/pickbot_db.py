"""
PostgreSQL에서 카페+리뷰 데이터를 조회합니다. (RAG 인덱싱용)
"""

from __future__ import annotations
import logging

import psycopg

from config import Settings

logger = logging.getLogger(__name__)

_SQL = """
SELECT
    r.review_id,
    c.cafe_id,
    c.name        AS cafe_name,
    c.address,
    r.content     AS review_text
FROM review r
JOIN cafe c ON c.cafe_id = r.cafe_id
WHERE c.status = 'APPROVED'
  AND r.content IS NOT NULL
ORDER BY c.cafe_id, r.review_id
"""


def fetch_reviews_for_index(settings: Settings) -> list[dict]:
    """APPROVED 카페의 리뷰를 전체 조회하여 dict 목록으로 반환합니다."""
    try:
        with psycopg.connect(
            host=settings.db_host,
            port=settings.db_port,
            dbname=settings.db_name,
            user=settings.db_user,
            password=settings.db_password,
        ) as conn:
            with conn.cursor() as cur:
                cur.execute(_SQL)
                rows = cur.fetchall()

        result = [
            {
                "review_id": row[0],
                "cafe_id":   row[1],
                "cafe_name": row[2],
                "address":   row[3] or "",
                "review":    row[4],
            }
            for row in rows
        ]
        logger.info("DB에서 리뷰 %d건 조회 완료", len(result))
        return result

    except Exception as e:
        logger.error("DB 조회 실패: %s", e)
        raise


# ── 픽봇 추천용 조회 ──────────────────────────────────────────────────────────

_DIRECTORY_SQL = """
SELECT
    c.cafe_id,
    c.name     AS cafe_name,
    c.address,
    count(r.review_id) FILTER (WHERE r.content IS NOT NULL) AS review_count,
    count(r.review_id) FILTER (WHERE r.sentiment = 'GOOD')  AS good_count
FROM cafe c
LEFT JOIN review r ON r.cafe_id = c.cafe_id
WHERE c.status = 'APPROVED'
GROUP BY c.cafe_id, c.name, c.address
"""

# 카페마다 긍정(GOOD) 리뷰를 우선, 그중 최신 리뷰 1건
_REPRESENTATIVE_SQL = """
SELECT DISTINCT ON (r.cafe_id) r.cafe_id, r.content
FROM review r
WHERE r.cafe_id = ANY(%s) AND r.content IS NOT NULL
ORDER BY r.cafe_id, (r.sentiment = 'GOOD') DESC NULLS LAST, r.created_at DESC NULLS LAST
"""


def _connect(settings: Settings):
    return psycopg.connect(
        host=settings.db_host,
        port=settings.db_port,
        dbname=settings.db_name,
        user=settings.db_user,
        password=settings.db_password,
    )


def fetch_cafe_directory(settings: Settings) -> list[dict]:
    """승인된 카페 전체의 주소와 리뷰 수·긍정 리뷰 수. 지역 필터와 조건 없는 순위에 쓴다."""
    with _connect(settings) as conn, conn.cursor() as cur:
        cur.execute(_DIRECTORY_SQL)
        return [
            {"cafe_id": row[0], "cafe_name": row[1], "address": row[2] or "",
             "review_count": row[3], "good_count": row[4]}
            for row in cur.fetchall()
        ]


def fetch_representative_reviews(settings: Settings, cafe_ids: list[int]) -> dict[int, str]:
    """카페별 대표 리뷰 1건 (긍정 리뷰 우선, 최신순)."""
    if not cafe_ids:
        return {}
    with _connect(settings) as conn, conn.cursor() as cur:
        cur.execute(_REPRESENTATIVE_SQL, (list(cafe_ids),))
        return {row[0]: row[1] for row in cur.fetchall()}
