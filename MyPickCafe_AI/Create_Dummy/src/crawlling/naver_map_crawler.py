"""
네이버 지도 카페 리뷰 크롤러

동작 흐름:
  1. map.naver.com 검색 → 스크롤하며 place_id + 카페명 수집
  2. 각 카페마다 map.naver.com/place/{id}?placePath=/review/visitor 직접 이동
     → entryIframe이 리뷰 페이지를 proper context로 로드
  3. 키워드 필터 버튼 순회 → 상위 N개 리뷰 텍스트 수집
"""
from __future__ import annotations

import asyncio
import logging
import random
import re
from dataclasses import asdict, dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Optional

from playwright.async_api import FrameLocator, Page, async_playwright


# ---------------------------------------------------------------------------
# 데이터 모델
# ---------------------------------------------------------------------------

@dataclass
class CategoryReviews:
    name: str
    vote_count: int
    reviews: list[str] = field(default_factory=list)


@dataclass
class CafeResult:
    name: str
    place_id: str
    categories: list[CategoryReviews] = field(default_factory=list)

    def to_dict(self) -> dict:
        return asdict(self)


# ---------------------------------------------------------------------------
# 봇 탐지 우회용 랜덤 풀
# ---------------------------------------------------------------------------

_VIEWPORTS = [
    {"width": 1280, "height": 800},
    {"width": 1366, "height": 768},
    {"width": 1440, "height": 900},
    {"width": 1536, "height": 864},
    {"width": 1920, "height": 1080},
]

_USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36",
]


def _pick_slow_mo() -> int:
    tier = random.choices([1, 2, 3, 4], weights=[70, 20, 8, 2])[0]
    if tier == 1:
        return random.randint(157, 394)
    if tier == 2:
        return random.choice([random.randint(148, 156), random.randint(395, 413)])
    if tier == 3:
        return random.choice([random.randint(121, 147), random.randint(414, 435)])
    return random.choice([random.randint(103, 120), random.randint(436, 458)])


def _pick_cafe_delay() -> float:
    tier = random.choices([1, 2, 3, 4], weights=[70, 20, 8, 2])[0]
    if tier == 1:
        return random.uniform(2.26, 4.32)
    if tier == 2:
        return random.choice([random.uniform(2.13, 2.25), random.uniform(4.33, 4.51)])
    if tier == 3:
        return random.choice([random.uniform(1.97, 2.12), random.uniform(4.52, 4.75)])
    return random.choice([random.uniform(1.83, 1.96), random.uniform(4.76, 5.12)])


def _pick_btn_delay() -> float:
    return random.uniform(0.8, 2.0)


# ---------------------------------------------------------------------------
# 크롤러
# ---------------------------------------------------------------------------

class NaverMapCrawler:
    _SEARCH_URL = "https://map.naver.com/p/search/{query}"
    # 카페 클릭 후 page.url 패턴: .../place/{place_id}?...
    _PLACE_URL  = "https://map.naver.com/p/search/{query}/place/{place_id}?placePath=/review/visitor"

    _SEL_LIST_ITEM = "li.UEzoS"
    _SEL_CAFE_LINK = "a.YTJkH"
    _SEL_CAFE_NAME = "span.TYaxT"
    # 키워드·리뷰 선택자 — 실제 DOM 확인 후 수정 필요
    _SEL_KW_BTN      = "div.zRM7B button, div.place_section_content button.lEVhH, div.YsRMx button"
    _SEL_REVIEW_TEXT = "span.zPfVt, div.pui__xoxo38 span, .place_review_text span"

    def __init__(
        self,
        query: str = "카페",
        top_n_cafes: int = 5,
        top_n_reviews: int = 5,
        headless: bool = False,
        log_dir: str = "logs",
    ):
        self.query = query
        self.top_n_cafes = top_n_cafes
        self.top_n_reviews = top_n_reviews
        self.headless = headless
        self._log_path = Path(log_dir) / f"crawl_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
        self._logger = self._setup_logger()

    # ------------------------------------------------------------------
    # 로거
    # ------------------------------------------------------------------

    def _setup_logger(self) -> logging.Logger:
        self._log_path.parent.mkdir(parents=True, exist_ok=True)
        logger = logging.getLogger(f"crawler_{id(self)}")
        logger.setLevel(logging.INFO)
        handler = logging.FileHandler(self._log_path, encoding="utf-8")
        handler.setFormatter(logging.Formatter("%(asctime)s  %(message)s", datefmt="%Y-%m-%d %H:%M:%S"))
        logger.addHandler(handler)
        return logger

    def _log_cafe(self, idx: int, total: int, result: Optional[CafeResult], elapsed: float) -> None:
        if result is None:
            self._logger.info(f"[{idx:03d}/{total}] FAIL  elapsed={elapsed:.1f}s")
            return
        total_reviews = sum(len(c.reviews) for c in result.categories)
        cat_summary = ", ".join(
            f"{c.name}({c.vote_count}표/{len(c.reviews)}개)" for c in result.categories
        )
        self._logger.info(
            f"[{idx:03d}/{total}] OK  {result.name}  id={result.place_id}  "
            f"카테고리={len(result.categories)}개  리뷰={total_reviews}개  elapsed={elapsed:.1f}s\n"
            f"            카테고리 상세: {cat_summary}"
        )

    # ------------------------------------------------------------------
    # public
    # ------------------------------------------------------------------

    async def run(self) -> list[CafeResult]:
        self._logger.info(f"크롤 시작  query={self.query}  cafes={self.top_n_cafes}  reviews={self.top_n_reviews}")
        print(f"[로그] {self._log_path}")

        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=self.headless, slow_mo=_pick_slow_mo())
            context = await browser.new_context(
                user_agent=random.choice(_USER_AGENTS),
                viewport=random.choice(_VIEWPORTS),
                locale="ko-KR",
            )
            page = await context.new_page()
            try:
                results = await self._crawl(page)
            finally:
                await browser.close()

        self._logger.info(f"크롤 완료  수집={len(results)}개")
        return results

    # ------------------------------------------------------------------
    # Phase 1: place_id 수집
    # ------------------------------------------------------------------

    async def _crawl(self, page: Page) -> list[CafeResult]:
        url = self._SEARCH_URL.format(query=self.query)
        print(f"[검색] {url}")
        await page.goto(url, wait_until="domcontentloaded", timeout=30_000)
        await page.wait_for_timeout(2_000)

        search_fl  = page.frame_locator("iframe#searchIframe")
        iframe_el  = await page.query_selector("iframe#searchIframe")
        raw_frame  = await iframe_el.content_frame() if iframe_el else None

        await self._scroll_to_load(raw_frame, search_fl, self.top_n_cafes)

        cafe_list = await self._collect_cafe_list(page, search_fl)
        targets   = cafe_list[: self.top_n_cafes]
        print(f"[목록] {len(targets)}개 카페 확보\n")

        # Phase 2: 각 카페 리뷰 수집
        results: list[CafeResult] = []
        for idx, (place_id, name) in enumerate(targets, 1):
            print(f"── [{idx}/{len(targets)}] {name}  (id={place_id})")
            t_start = asyncio.get_event_loop().time()

            result = await self._process_cafe(page, place_id, name)
            elapsed = asyncio.get_event_loop().time() - t_start

            self._log_cafe(idx, len(targets), result, elapsed)
            if result:
                results.append(result)

            if idx < len(targets):
                delay = _pick_cafe_delay()
                print(f"  (다음 카페까지 {delay:.1f}초 대기)\n")
                await asyncio.sleep(delay)

        return results

    async def _collect_cafe_list(self, page: Page, search_fl: FrameLocator) -> list[tuple[str, str]]:
        """검색 결과 클릭 → page.url에서 place_id 추출"""
        items = search_fl.locator(self._SEL_LIST_ITEM)
        total = await items.count()
        needed = self.top_n_cafes

        collected: list[tuple[str, str]] = []
        for i in range(total):
            if len(collected) >= needed * 2:
                break
            item = items.nth(i)
            try:
                name = (await item.locator(self._SEL_CAFE_NAME).first.text_content(timeout=2_000) or "").strip()
                if not name:
                    continue

                await item.locator(self._SEL_CAFE_LINK).first.click(timeout=6_000)
                await page.wait_for_timeout(1_500)

                # page.url에서 place_id 추출
                m = re.search(r"/place/(\d+)", page.url)
                if not m:
                    continue
                place_id = m.group(1)

                collected.append((place_id, name))
                print(f"  {len(collected):02d}. {name}  ({place_id})")
            except Exception:
                continue

        return collected

    async def _scroll_to_load(self, raw_frame, search_fl: FrameLocator, needed: int) -> None:
        if raw_frame is None:
            return
        try:
            await search_fl.locator(self._SEL_LIST_ITEM).first.wait_for(timeout=15_000)
        except Exception:
            print("[오류] 검색 결과 로드 실패")
            return

        prev_count = 0
        for _ in range((needed // 8) + 6):
            current = await search_fl.locator(self._SEL_LIST_ITEM).count()
            if current >= needed or (current == prev_count and current > 0):
                break
            prev_count = current
            await raw_frame.evaluate("""
                const targets = [
                    document.querySelector('div[class*="search_list"]'),
                    document.querySelector('div[class*="list_section"]'),
                    document.querySelector('ul'),
                    document.body
                ];
                const el = targets.find(t => t) || document.body;
                el.scrollTop = el.scrollHeight;
                window.scrollTo(0, document.body.scrollHeight);
            """)
            await asyncio.sleep(random.uniform(1.2, 2.5))

        final = await search_fl.locator(self._SEL_LIST_ITEM).count()
        print(f"  [스크롤 완료] 검색 결과 {final}개 로드")

    # ------------------------------------------------------------------
    # Phase 2: 카페별 리뷰 수집
    # ------------------------------------------------------------------

    async def _process_cafe(self, page: Page, place_id: str, name: str) -> Optional[CafeResult]:
        # map.naver.com placePath로 리뷰 페이지 직접 이동 (pcmap 직접 접근 X)
        review_url = self._PLACE_URL.format(query=self.query, place_id=place_id)
        try:
            await page.goto(review_url, wait_until="domcontentloaded", timeout=30_000)
            await page.wait_for_timeout(4_000)
        except Exception as e:
            print(f"  [오류] 페이지 이동 실패: {e}")
            return None

        # 차단 감지
        ef_el = await page.query_selector("iframe#entryIframe")
        if ef_el:
            ef = await ef_el.content_frame()
            if ef:
                blocked = await ef.evaluate("""
                    () => document.body.innerText.includes('서비스 이용이 제한')
                """)
                if blocked:
                    print("  [경고] 네이버 차단 감지 — 30초 대기 후 재시도")
                    await asyncio.sleep(30)
                    await page.goto(review_url, wait_until="domcontentloaded", timeout=30_000)
                    await page.wait_for_timeout(3_000)

        entry_fl = page.frame_locator("iframe#entryIframe")
        categories = await self._collect_categories(page, entry_fl)
        return CafeResult(name=name, place_id=place_id, categories=categories)

    # ------------------------------------------------------------------
    # 카테고리 & 리뷰 텍스트
    # ------------------------------------------------------------------

    async def _collect_categories(self, page: Page, entry_fl: FrameLocator) -> list[CategoryReviews]:
        try:
            await entry_fl.locator("div.place_section_content, div.zRM7B, div.YsRMx").first.wait_for(timeout=10_000)
        except Exception:
            print("  [경고] 키워드 섹션 로드 실패")
            return []

        kw_btns = entry_fl.locator(self._SEL_KW_BTN)
        btn_count = await kw_btns.count()
        print(f"  키워드 카테고리 {btn_count}개")

        results: list[CategoryReviews] = []
        for i in range(btn_count):
            btn = kw_btns.nth(i)
            try:
                label = (await btn.text_content(timeout=2_000) or "").strip()
                if not label:
                    continue
                parts = label.rsplit(" ", 1)
                cat_name   = parts[0].strip()
                vote_count = int(parts[1]) if len(parts) > 1 and parts[1].isdigit() else 0

                await btn.click(timeout=3_000)
                await page.wait_for_timeout(int(_pick_btn_delay() * 1000))

                reviews = await self._collect_review_texts(entry_fl)
                results.append(CategoryReviews(name=cat_name, vote_count=vote_count, reviews=reviews))
                print(f"    [{cat_name}] 투표 {vote_count}건 → 리뷰 {len(reviews)}개 수집")
            except Exception as e:
                print(f"    [오류] 카테고리 {i}: {e}")
                continue

        return results

    async def _collect_review_texts(self, entry_fl: FrameLocator) -> list[str]:
        review_els = entry_fl.locator(self._SEL_REVIEW_TEXT)
        count = await review_els.count()
        reviews: list[str] = []
        for i in range(count):
            if len(reviews) >= self.top_n_reviews:
                break
            try:
                text = (await review_els.nth(i).text_content(timeout=1_500) or "").strip()
                if len(text) > 5:
                    reviews.append(text)
            except Exception:
                continue
        return reviews
