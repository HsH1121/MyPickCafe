"""
MyPickCafe Review Tag AI — FastAPI 중계 서버
Spring Boot 백엔드 ↔ 로컬 AI 모델 서버 사이를 중계합니다.

엔드포인트:
  POST /review/analyze  — 리뷰 분석 (태그 + 감성 추출)
  GET  /health          — 헬스체크
"""

from __future__ import annotations
import logging

from fastapi import FastAPI
from fastapi.responses import JSONResponse

from config import Settings
from schemas import ReviewRequest, ReviewAnalyzeResponse
from ollama_client import call_ollama
from prompt_builder import (
    SYSTEM_PROMPT,
    build_user_message,
    ALLOWED_FACILITY_TAGS,
    ALLOWED_MENU_TAGS,
    ALLOWED_PURPOSE_TAGS,
    ALLOWED_MOOD_TAGS,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

settings = Settings()

app = FastAPI(
    title="MyPickCafe Review Tag AI",
    description="카페 리뷰 분석 중계 서버 (Spring Boot ↔ AI 모델 서버)",
    version="1.0.0",
)

_VALID_SENTIMENTS: frozenset[str] = frozenset({"GOOD", "BAD"})


@app.post(
    "/review/analyze",
    response_model=ReviewAnalyzeResponse,
    summary="리뷰 분석 — 태그 추출 및 감성 판별",
)
async def analyze_review(request: ReviewRequest) -> ReviewAnalyzeResponse:
    logger.info(
        "리뷰 분석 요청 — reviewId=%d, content_len=%d",
        request.reviewId, len(request.reviewText),
    )

    user_message = build_user_message(request)

    try:
        raw: dict = await call_ollama(
            system_prompt=SYSTEM_PROMPT,
            user_message=user_message,
            model=settings.ollama_model,
            base_url=settings.model_api_url,
            timeout=settings.ollama_timeout,
        )
    except Exception as exc:
        logger.warning("모델 호출 실패 — reviewId=%d, 빈 태그로 응답: %s", request.reviewId, exc)
        return ReviewAnalyzeResponse()

    def _extract_tags(key: str, allowed: frozenset[str]) -> list[str]:
        val = raw.get(key, [])
        if not isinstance(val, list):
            logger.warning("%s 필드가 리스트가 아님: %r — 빈 리스트로 대체", key, val)
            return []
        filtered = [t for t in val if isinstance(t, str) and t in allowed]
        excluded = [t for t in val if isinstance(t, str) and t not in allowed]
        if excluded:
            logger.warning("%s 허용 목록 외 태그 제외: %s", key, excluded)
        return filtered

    facility_tags = _extract_tags("FACILITY", ALLOWED_FACILITY_TAGS)
    menu_tags     = _extract_tags("MENU",     ALLOWED_MENU_TAGS)
    purpose_tags  = _extract_tags("PURPOSE",  ALLOWED_PURPOSE_TAGS)
    mood_tags     = _extract_tags("MOOD",     ALLOWED_MOOD_TAGS)

    ai_sentiment = raw.get("sentiment")
    if ai_sentiment not in _VALID_SENTIMENTS:
        if ai_sentiment is not None:
            logger.warning("AI sentiment 값 무효: %r — null로 대체", ai_sentiment)
        final_sentiment: str | None = None
    else:
        final_sentiment = ai_sentiment

    logger.info(
        "분석 완료 — reviewId=%d, FACILITY=%s, MENU=%s, PURPOSE=%s, MOOD=%s, sentiment=%s",
        request.reviewId, facility_tags, menu_tags, purpose_tags, mood_tags, final_sentiment,
    )
    return ReviewAnalyzeResponse(
        sentiment=final_sentiment,
        FACILITY=facility_tags,
        MENU=menu_tags,
        PURPOSE=purpose_tags,
        MOOD=mood_tags,
    )


@app.get("/health", summary="헬스체크")
async def health_check() -> JSONResponse:
    return JSONResponse({
        "status": "ok",
        "model":  settings.ollama_model,
    })
