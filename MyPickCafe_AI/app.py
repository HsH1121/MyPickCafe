"""
MyPickCafe AI — 통합 FastAPI 서버
ChatBot_AI + Review_Tag_AI 엔드포인트를 단일 서버에서 제공합니다.

엔드포인트:
  POST /chatbot/recommend   — AI 카페 추천 (RAG)
  POST /chatbot/reindex     — ChromaDB 재인덱싱
  POST /chatbot/index-one   — 단일 리뷰 ChromaDB upsert
  POST /chatbot/delete-one  — 단일 리뷰 ChromaDB 삭제
  POST /review/analyze      — 리뷰 분석 (태그 + 감성 추출)
  GET  /health              — 헬스체크
"""

from __future__ import annotations
import asyncio
import sys
import os
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

BASE = os.path.dirname(os.path.abspath(__file__))

# ── ChatBot_AI 모듈 로드 ─────────────────────────────────────────────────────
sys.path.insert(0, os.path.join(BASE, "ChatBot_AI"))

from config import Settings as ChatbotSettings
from schemas import ChatbotRequest, ChatbotResult, IndexOneRequest, DeleteOneRequest
from chatbot_rag import CafeRAG

# ── Review_Tag_AI 모듈 로드 (충돌 모듈 제거 후 재로드) ──────────────────────
for _mod in ["config", "schemas", "ollama_client"]:
    sys.modules.pop(_mod, None)

sys.path.insert(0, os.path.join(BASE, "Review_Tag_AI"))

from config import Settings as ReviewSettings
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

# ── 설정 및 로깅 ─────────────────────────────────────────────────────────────
chatbot_settings = ChatbotSettings()
review_settings  = ReviewSettings()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

cafe_rag: CafeRAG | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global cafe_rag
    cafe_rag = CafeRAG(chatbot_settings)
    if cafe_rag.indexed_count == 0:
        logger.info("ChromaDB가 비어 있어 초기 인덱싱을 시작합니다.")
        try:
            n = await asyncio.to_thread(cafe_rag.index_from_db)
            logger.info("초기 인덱싱 완료: %d건", n)
        except Exception as e:
            logger.error("초기 인덱싱 실패 (챗봇 비활성): %s", e)
    else:
        logger.info("ChromaDB 기존 인덱스 로드: %d건", cafe_rag.indexed_count)
    yield


app = FastAPI(
    title="MyPickCafe AI",
    description="챗봇 추천 + 리뷰 태그 분석 통합 FastAPI 서버",
    version="1.0.0",
    lifespan=lifespan,
)

_VALID_SENTIMENTS: frozenset[str] = frozenset({"GOOD", "BAD"})


# ── ChatBot 엔드포인트 ────────────────────────────────────────────────────────

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


@app.post("/chatbot/index-one", summary="단일 리뷰 ChromaDB upsert")
async def chatbot_index_one(request: IndexOneRequest) -> JSONResponse:
    if cafe_rag is None:
        raise HTTPException(status_code=503, detail="RAG 모듈이 초기화되지 않았습니다.")
    try:
        await asyncio.to_thread(
            cafe_rag.index_one,
            review_id=request.reviewId,
            cafe_id=request.cafeId,
            cafe_name=request.cafeName,
            address=request.address,
            review=request.reviewText,
        )
        return JSONResponse({"indexed": request.reviewId})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chatbot/delete-one", summary="단일 리뷰 ChromaDB 삭제")
async def chatbot_delete_one(request: DeleteOneRequest) -> JSONResponse:
    if cafe_rag is None:
        raise HTTPException(status_code=503, detail="RAG 모듈이 초기화되지 않았습니다.")
    try:
        await asyncio.to_thread(cafe_rag.delete_one, request.reviewId)
        return JSONResponse({"deleted": request.reviewId})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chatbot/reindex", summary="ChromaDB 재인덱싱 (DB 변경 시 수동 갱신)")
async def chatbot_reindex() -> JSONResponse:
    if cafe_rag is None:
        raise HTTPException(status_code=503, detail="RAG 모듈이 초기화되지 않았습니다.")
    try:
        n = await asyncio.to_thread(cafe_rag.index_from_db)
        return JSONResponse({"indexed": n})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ── Review 엔드포인트 ─────────────────────────────────────────────────────────

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
            model=review_settings.ollama_model,
            base_url=review_settings.model_api_url,
            timeout=review_settings.ollama_timeout,
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


# ── 공통 헬스체크 ─────────────────────────────────────────────────────────────

@app.get("/health", summary="헬스체크")
async def health_check() -> JSONResponse:
    indexed = cafe_rag.indexed_count if cafe_rag else 0
    return JSONResponse({
        "status":        "ok",
        "chatbot_model": chatbot_settings.ollama_model,
        "embed_model":   chatbot_settings.embed_model,
        "indexed":       indexed,
        "review_model":  review_settings.ollama_model,
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)
