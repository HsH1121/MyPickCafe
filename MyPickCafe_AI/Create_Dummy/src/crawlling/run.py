import asyncio
import json
from pathlib import Path

from naver_map_crawler import NaverMapCrawler

# ── 여기서 설정 ──────────────────────────────────────
QUERY       = "카페"               # 검색 키워드
TOP_CAFES   = 100                    # 수집할 카페 수
TOP_REVIEWS = 15                    # 카테고리별 리뷰 수
OUTPUT      = "crawl_result.json"  # 출력 파일 경로
# ─────────────────────────────────────────────────────


async def main() -> None:
    crawler = NaverMapCrawler(
        query=QUERY,
        top_n_cafes=TOP_CAFES,
        top_n_reviews=TOP_REVIEWS,
    )

    results = await crawler.run()

    out_path = Path(OUTPUT)
    out_path.write_text(
        json.dumps([r.to_dict() for r in results], ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"\n완료: {len(results)}개 카페 → {out_path}")
    for cafe in results:
        total = sum(len(c.reviews) for c in cafe.categories)
        print(f"  {cafe.name}: 카테고리 {len(cafe.categories)}개, 리뷰 {total}개")


if __name__ == "__main__":
    asyncio.run(main())
