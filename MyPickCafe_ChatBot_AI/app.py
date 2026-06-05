"""
MyPickCafe ChatBot AI — FastAPI 중계 서버
Spring Boot 백엔드 ↔ 로컬 AI 모델 서버 사이를 중계합니다.

엔드포인트:
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
from schemas import ChatbotRequest, ChatbotResult, IndexOneRequest, DeleteOneRequest
from chatbot_rag import CafeRAG

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
    title="MyPickCafe ChatBot AI",
    description="AI 카페 추천 중계 서버 (Spring Boot ↔ AI 모델 서버)",
    version="1.0.0",
    lifespan=lifespan,
)


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
        cafe_rag.index_one(
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
        cafe_rag.delete_one(request.reviewId)
        return JSONResponse({"deleted": request.reviewId})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chatbot/reindex", summary="ChromaDB 재인덱싱 (DB 변경 시 수동 갱신)")
async def chatbot_reindex() -> JSONResponse:
    if cafe_rag is None:
        raise HTTPException(status_code=503, detail="RAG 모듈이 초기화되지 않았습니다.")
    try:
        n = cafe_rag.index_from_db()
        return JSONResponse({"indexed": n})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health", summary="헬스체크")
async def health_check() -> JSONResponse:
    indexed = cafe_rag.indexed_count if cafe_rag else 0
    return JSONResponse({
        "status":  "ok",
        "model":   settings.ollama_model,
        "embed":   settings.embed_model,
        "indexed": indexed,
    })
