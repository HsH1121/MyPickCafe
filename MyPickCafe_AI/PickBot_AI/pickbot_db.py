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
