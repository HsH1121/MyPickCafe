"""
카페 추천 RAG 파이프라인

1. index_from_db()  — Oracle DB 리뷰를 ChromaDB에 임베딩+저장
2. recommend()      — 쿼리 임베딩 → 유사도 검색 → Qwen 추천 생성
"""

from __future__ import annotations
import json
import logging

import chromadb
from chromadb.utils.embedding_functions import OllamaEmbeddingFunction

from config import Settings
from chatbot_db import fetch_reviews_for_index
from ollama_client import call_ollama

logger = logging.getLogger(__name__)

_COLLECTION = "cafe_reviews"

_SYSTEM_PROMPT = """당신은 카페 추천 전문 AI입니다.
검색된 카페 정보를 바탕으로 사용자 질문에 맞는 카페를 추천합니다.
반드시 JSON 객체 하나만 반환하세요. 설명 텍스트 절대 금지.

형식:
{"results": [{"cafeId": <정수>, "cafeName": "<이름>", "address": "<주소>", "snippet": "<추천 이유 1-2문장>"}]}

조건에 맞는 카페가 없으면 {"results": []} 을 반환하세요."""


class CafeRAG:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._emb_fn = OllamaEmbeddingFunction(
            url=f"{settings.model_api_url}/api/embeddings",
            model_name=settings.embed_model,
        )
        self._client = chromadb.PersistentClient(path=settings.chroma_path)
        self._col = self._client.get_or_create_collection(
            name=_COLLECTION,
            embedding_function=self._emb_fn,
            metadata={"hnsw:space": "cosine"},
        )

    # ------------------------------------------------------------------
    # 인덱싱
    # ------------------------------------------------------------------
    def index_from_db(self) -> int:
        """DB 리뷰 중 아직 ChromaDB에 없는 것만 임베딩+저장."""
        rows = fetch_reviews_for_index(self.settings)
        if not rows:
            logger.warning("인덱싱할 리뷰가 없습니다.")
            return 0

        existing_ids = set(self._col.get(include=[])["ids"])

        documents, metadatas, ids = [], [], []
        for r in rows:
            rid = f"review_{r['review_id']}"
            if rid in existing_ids:
                continue
            documents.append(f"{r['cafe_name']} | {r['address']} | {r['review']}")
            metadatas.append({
                "cafe_id":   str(r["cafe_id"]),
                "cafe_name": r["cafe_name"],
                "address":   r["address"],
                "review":    r["review"][:500],
            })
            ids.append(rid)

        if not ids:
            logger.info("새로 인덱싱할 리뷰가 없습니다. (기존 %d건)", len(existing_ids))
            return 0

        batch = 500
        for i in range(0, len(ids), batch):
            self._col.add(
                documents=documents[i : i + batch],
                metadatas=metadatas[i : i + batch],
                ids=ids[i : i + batch],
            )

        logger.info("ChromaDB 인덱싱 완료: 신규 %d건 추가 (기존 %d건)", len(ids), len(existing_ids))
        return len(ids)

    def index_one(self, review_id: int, cafe_id: int, cafe_name: str, address: str, review: str) -> None:
        """단일 리뷰를 ChromaDB에 upsert."""
        doc = f"{cafe_name} | {address} | {review}"
        self._col.upsert(
            documents=[doc],
            metadatas=[{
                "cafe_id":   str(cafe_id),
                "cafe_name": cafe_name,
                "address":   address,
                "review":    review[:500],
            }],
            ids=[f"review_{review_id}"],
        )

    def delete_one(self, review_id: int) -> None:
        """단일 리뷰를 ChromaDB에서 삭제."""
        self._col.delete(ids=[f"review_{review_id}"])

    @property
    def indexed_count(self) -> int:
        return self._col.count()

    # ------------------------------------------------------------------
    # 추천
    # ------------------------------------------------------------------
    async def recommend(self, query: str, top_n: int = 5) -> list[dict]:
        """쿼리 → 유사도 검색 → Qwen 추천 → ChatbotResult dict 목록 반환."""
        if self._col.count() == 0:
            logger.warning("ChromaDB가 비어 있습니다. 먼저 /chatbot/reindex를 호출하세요.")
            return []

        # 1. 벡터 검색 (top_n * 3개 가져와서 Qwen이 선별)
        fetch_k = min(top_n * 3, 15)
        results = self._col.query(query_texts=[query], n_results=fetch_k)

        docs      = results["documents"][0]
        metas     = results["metadatas"][0]
        distances = results["distances"][0]

        # 2. 카페별 최고 유사도 점수로 그룹핑
        cafe_map: dict[str, dict] = {}
        for meta, dist in zip(metas, distances):
            cid   = meta["cafe_id"]
            score = 1.0 - dist  # cosine distance → similarity
            if cid not in cafe_map or cafe_map[cid]["score"] < score:
                cafe_map[cid] = {
                    "cafe_id":   int(cid),
                    "cafe_name": meta["cafe_name"],
                    "address":   meta["address"],
                    "review":    meta["review"],
                    "score":     score,
                }

        top_cafes = sorted(cafe_map.values(), key=lambda x: x["score"], reverse=True)[:top_n]

        # 3. Qwen 컨텍스트 구성
        context = "\n\n".join(
            f"[카페{i}] ID={c['cafe_id']}, 이름={c['cafe_name']}, 주소={c['address']}\n"
            f"리뷰: {c['review']}"
            for i, c in enumerate(top_cafes, 1)
        )
        user_msg = (
            f"사용자 질문: {query}\n\n"
            f"검색된 카페 정보:\n{context}\n\n"
            "위 카페 중 사용자 질문에 가장 잘 맞는 카페를 JSON으로 반환하세요."
        )

        # 4. Qwen 호출
        try:
            raw = await call_ollama(
                system_prompt=_SYSTEM_PROMPT,
                user_message=user_msg,
                model=self.settings.ollama_model,
                base_url=self.settings.model_api_url,
                timeout=self.settings.ollama_timeout,
            )
            items = raw.get("results", [])
            if not isinstance(items, list):
                raise ValueError(f"results 필드가 리스트가 아님: {items!r}")
        except Exception as e:
            logger.warning("Qwen 호출 실패, 검색 결과 직접 반환: %s", e)
            return [
                {
                    "cafeId":   c["cafe_id"],
                    "cafeName": c["cafe_name"],
                    "address":  c["address"],
                    "snippet":  c["review"][:150],
                    "score":    round(c["score"], 4),
                }
                for c in top_cafes
            ]

        # 5. Qwen 응답 → ChatbotResult 형태로 변환
        score_by_id = {str(c["cafe_id"]): c["score"] for c in top_cafes}
        output = []
        for item in items:
            try:
                cid = int(item["cafeId"])
                output.append({
                    "cafeId":   cid,
                    "cafeName": str(item.get("cafeName", "")),
                    "address":  str(item.get("address", "")),
                    "snippet":  str(item.get("snippet", "")),
                    "score":    round(score_by_id.get(str(cid), 0.0), 4),
                })
            except (KeyError, ValueError, TypeError) as e:
                logger.warning("Qwen 응답 항목 파싱 오류: %s — %r", e, item)

        return output
