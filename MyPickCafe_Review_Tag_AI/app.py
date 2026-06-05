"""
MyPickCafe Review Tag AI — FastAPI 중계 서버
Spring Boot 백엔드 ↔ 로컬 AI 모델 서버 사이를 중계합니다.

엔드포인트:
  POST /review/analyze     — 리뷰 분석 (태그 + 감성 추출)
  POST /chatbot/recommend  — AI 카페 추천 (RAG)
  POST /chatbot/reindex    — ChromaDB 재인덱싱
  GET  /health             — 헬스체크
"""

from __future__ import annotations
import logging

from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

from config import Settings
from schemas import ReviewRequest, ReviewAnalyzeResponse, ChatbotRequest, ChatbotResult
from ollama_client import call_ollama
from chatbot_rag import CafeRAG
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
cafe_rag: CafeRAG | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global cafe_rag
    cafe_rag = CafeRAG(settings)
    if cafe_rag.indexed_count == 0:
        logger.info("ChromaDB가 비어 있어 초기 인덱싱을 시작합니다.")
        try:
            n = cafe_rag.index_from_db()
            logger.info("초기 인덱싱 완료: %d건", n)
        except Exception as e:
            logger.error("초기 인덱싱 실패 (챗봇 비활성): %s", e)
    else:
        logger.info("ChromaDB 기존 인덱스 로드: %d건", cafe_rag.indexed_count)
    yield


app = FastAPI(
    title="MyPickCafe Review Tag AI",
    description="카페 리뷰 분석 중계 서버 (Spring Boot ↔ AI 모델 서버)",
    version="1.0.0",
    lifespan=lifespan,
)

_VALID_SENTIMENTS: frozenset[str] = frozenset({"GOOD", "BAD"})


# ---------------------------------------------------------------------------
# 엔드포인트
# ---------------------------------------------------------------------------
@app.post(
    "/review/analyze",
    response_model=ReviewAnalyzeResponse,
    summary="리뷰 분석 — 태그 추출 및 감성 판별",
)
async def analyze_review(request: ReviewRequest) -> ReviewAnalyzeResponse:
    """
    Spring Boot 백엔드에서 전달한 리뷰를 AI 모델 서버로 분석합니다.

    - 모델 호출 실패 시 빈 태그 리스트로 응답합니다.
    - 허용 목록 외 태그는 카테고리별로 필터링 후 반환합니다.
    - 감성이 GOOD/BAD 외 값이면 null로 처리합니다.
    """
    logger.info(
        "리뷰 분석 요청 — reviewId=%d, content_len=%d",
        request.reviewId, len(request.reviewText),
    )

    # ── 1. 프롬프트 구성 ────────────────────────────────────────────────────
    user_message = build_user_message(request)

    # ── 2. AI 모델 호출 ─────────────────────────────────────────────────────
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

    # ── 3. 태그 카테고리별 수집 + 허용 목록 필터링 ──────────────────────────
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

    # ── 4. sentiment 검증 ──────────────────────────────────────────────────
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


# ---------------------------------------------------------------------------
# 챗봇 추천 (RAG)
# ---------------------------------------------------------------------------
@app.post(
    "/chatbot/recommend",
    response_model=list[ChatbotResult],
    summary="AI 카페 추천 — RAG (임베딩 검색 + Qwen 생성)",
)
async def chatbot_recommend(request: ChatbotRequest) -> list[ChatbotResult]:
    if cafe_rag is None:
        raise HTTPException(status_code=503, detail="RAG 모듈이 초기화되지 않았습니다.")
    results = await cafe_rag.recommend(request.query)
    return [ChatbotResult(**r) for r in results]


@app.post("/chatbot/reindex", summary="ChromaDB 재인덱싱 (DB 변경 시 수동 갱신)")
async def chatbot_reindex() -> JSONResponse:
    if cafe_rag is None:
        raise HTTPException(status_code=503, detail="RAG 모듈이 초기화되지 않았습니다.")
    try:
        n = cafe_rag.index_from_db()
        return JSONResponse({"indexed": n})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ---------------------------------------------------------------------------
# 헬스체크
# ---------------------------------------------------------------------------
@app.get("/health", summary="헬스체크")
async def health_check() -> JSONResponse:
    indexed = cafe_rag.indexed_count if cafe_rag else 0
    return JSONResponse({
        "status":  "ok",
        "model":   settings.ollama_model,
        "embed":   settings.embed_model,
        "indexed": indexed,
    })
