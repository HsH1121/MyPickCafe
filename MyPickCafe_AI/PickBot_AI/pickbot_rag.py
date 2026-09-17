"""
카페 추천 RAG 파이프라인

1. index_from_db()  — PostgreSQL 리뷰를 ChromaDB에 임베딩+저장
2. recommend()      — 질문 분해(LLM 1차) → 주소로 지역 필터 → 조건 문장으로 리뷰 유사도 검색 → LLM 2차 추천

카페 이름·주소는 판단에 섞지 않는다. 임베딩 문서도, LLM 에 보여주는 내용도 리뷰 본문뿐이다.
이름·주소를 섞으면 지역·이름 단어가 리뷰 내용과 무관하게 유사도를 끌어올리고,
분위기·메뉴를 묻는 질문에서는 리뷰 신호가 희석된다.
"""

from __future__ import annotations
import asyncio
import json
import logging
import os
import time

import chromadb
from chromadb import Documents, EmbeddingFunction, Embeddings
import httpx

from config import Settings
from pickbot_db import fetch_cafe_directory, fetch_representative_reviews, fetch_reviews_for_index
from llm_client import call_llm
from query_parser import ParsedQuery, parse_query


# 이 개수 이하의 임베딩 요청(검색 쿼리, 리뷰 1건 upsert)은 CPU 로 돌린다.
# 로컬에서는 bge-m3 와 qwen2.5:14b 가 VRAM 에 함께 올라가지 못해, GPU 로 임베딩하면
# 추천 요청마다 qwen 을 내리고 bge-m3 를 올렸다가 다시 qwen 을 올리느라 수 초가 든다.
# CPU 로 돌리면 두 모델이 동시에 상주하고 쿼리 1건은 로드 후 약 0.12s 다.
# CPU·GPU 임베딩의 코사인 유사도는 1.0 이라 GPU 로 만든 기존 인덱스를 그대로 쓴다.
# 대량 인덱싱(500건 배치)은 GPU 로 둔다.
_CPU_EMBED_MAX_BATCH = 16

# 대량 임베딩은 GPU 를 명시적으로 요청한다. 옵션 없이 보내면 Ollama 가 직전 쿼리 임베딩으로
# CPU 에 올라가 있던 bge-m3 를 그대로 재사용해, 500건 배치가 60초 타임아웃을 넘긴다
# (측정: 50건 CPU 4.96s / GPU 0.70s). num_gpu 를 크게 주면 모든 레이어를 GPU 에 올린다.
_GPU_ALL_LAYERS = 999


class _OllamaEmbeddingFunction(EmbeddingFunction):
    """Ollama /api/embed 엔드포인트용 범용 임베딩 함수."""

    def __init__(self, base_url: str, model: str, small_batches_on_cpu: bool = False) -> None:
        self._base_url = base_url.rstrip("/")
        self._model = model
        self._small_batches_on_cpu = small_batches_on_cpu
        # 요청마다 클라이언트를 만들면 생성(SSL 설정 로딩)에만 약 0.19s 가 든다.
        self._http = httpx.Client(timeout=60)

    def _post(self, texts: list[str], num_gpu: int | None) -> httpx.Response:
        body: dict = {"model": self._model, "input": texts}
        if num_gpu is not None:
            body["options"] = {"num_gpu": num_gpu}
        return self._http.post(f"{self._base_url}/api/embed", json=body)

    def _embed(self, texts: list[str]) -> Embeddings:
        if self._small_batches_on_cpu and len(texts) <= _CPU_EMBED_MAX_BATCH:
            resp = self._post(texts, num_gpu=0)
            resp.raise_for_status()
            return resp.json()["embeddings"]
        gpu = _GPU_ALL_LAYERS if self._small_batches_on_cpu else None
        return self._embed_with_nan_fallback(texts, gpu)

    def _embed_with_nan_fallback(self, texts: list[str], num_gpu: int | None) -> Embeddings:
        """GPU 로 임베딩하되, NaN 이 나오는 문서만 골라 CPU 로 다시 임베딩한다.

        일부 리뷰(영문 문장, URL 만 있는 리뷰 등)는 GPU 에서 bge-m3 결과에 NaN 이 생겨
        Ollama 가 "json: unsupported value: NaN" 으로 500 을 낸다. 같은 문서가 CPU 에서는
        정상이고, 한 건 때문에 500건 묶음 전체가 실패한다. 묶음을 통째로 CPU 로 돌리면
        60초 타임아웃을 넘기므로, 반씩 나눠 문제 문서만 찾아 CPU 로 처리한다.
        NaN 이 아닌 500 은 진짜 장애일 수 있어 그대로 올린다.
        """
        resp = self._post(texts, num_gpu)
        if resp.status_code == 500 and "NaN" in resp.text:
            if len(texts) == 1:
                logger.warning("GPU 임베딩 결과에 NaN — 이 문서만 CPU 로 재시도: %r", texts[0][:80])
                cpu = self._post(texts, num_gpu=0)
                cpu.raise_for_status()
                return cpu.json()["embeddings"]
            mid = len(texts) // 2
            return (self._embed_with_nan_fallback(texts[:mid], num_gpu)
                    + self._embed_with_nan_fallback(texts[mid:], num_gpu))
        resp.raise_for_status()
        return resp.json()["embeddings"]

    def __call__(self, input: Documents) -> Embeddings:
        return self._embed(list(input))

    def embed_query(self, query: str) -> list[float]:
        return self._embed([query])[0]

logger = logging.getLogger(__name__)

# --- RAG 선택 전용 파일 로거 ---
_LOG_DIR = os.path.join(os.path.dirname(__file__), "logs")
os.makedirs(_LOG_DIR, exist_ok=True)

_rag_logger = logging.getLogger("rag_selection")
_rag_logger.setLevel(logging.DEBUG)
_rag_logger.propagate = False  # 루트 로거로 전파 차단

_fh = logging.FileHandler(
    os.path.join(_LOG_DIR, "rag_selection.log"),
    encoding="utf-8",
)
_fh.setFormatter(logging.Formatter("%(asctime)s\n%(message)s"))
_rag_logger.addHandler(_fh)

_COLLECTION = "cafe_reviews"

# 응답 notice 값 — 사용자가 말한 지역에 해당하는 카페가 데이터에 없음
NOTICE_REGION_NOT_FOUND = "REGION_NOT_FOUND"

# 카페 목록(주소·리뷰 수) 캐시 유지 시간. 요청마다 DB 를 조회하지 않기 위함.
_DIRECTORY_TTL_SEC = 300

_SYSTEM_PROMPT = """당신은 카페 추천 전문 AI입니다.
검색된 카페들의 리뷰만을 근거로 사용자 질문에 맞는 카페를 추천합니다.
카페 이름과 위치는 주어지지 않습니다. 추측해서 쓰지 마세요.
반드시 JSON 객체 하나만 반환하세요. 설명 텍스트 절대 금지.

형식:
{"results": [{"cafeId": <정수>, "snippet": "<리뷰에 근거한 추천 이유 1-2문장>"}]}

조건에 맞는 카페가 없으면 {"results": []} 을 반환하세요."""


class CafeRAG:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._directory: list[dict] = []
        self._directory_loaded_at: float | None = None
        self._emb_fn = _OllamaEmbeddingFunction(
            base_url=settings.embed_base_url,
            model=settings.embed_model,
            small_batches_on_cpu=settings.embed_small_batches_on_cpu,
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
            documents.append(r["review"])  # 리뷰 본문만 임베딩 (이름·주소는 메타데이터에만)
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
        """단일 리뷰를 ChromaDB에 upsert. 임베딩은 리뷰 본문만, 이름·주소는 메타데이터에만 둔다."""
        self._col.upsert(
            documents=[review],
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

    def reset_index(self) -> None:
        """ChromaDB 컬렉션을 전체 초기화합니다."""
        self._client.delete_collection(self._col.name)
        self._col = self._client.get_or_create_collection(
            name=_COLLECTION,
            embedding_function=self._emb_fn,
            metadata={"hnsw:space": "cosine"},
        )

    @property
    def indexed_count(self) -> int:
        return self._col.count()

    # ------------------------------------------------------------------
    # 추천
    # ------------------------------------------------------------------
    async def recommend(self, query: str, top_n: int = 5) -> dict:
        """질문 → 지역·조건 분해(LLM 1차) → 주소 필터 → 리뷰 유사도 검색 → LLM 2차 추천.

        반환: {"results": [PickBotResult dict, ...], "notice": None | NOTICE_*}
        """
        if self._col.count() == 0:
            logger.warning("ChromaDB가 비어 있습니다. 먼저 /pickbot/reindex를 호출하세요.")
            return {"results": [], "notice": None}

        # 1. 질문 분해 — 지역은 카페 주소에 실제로 있는 구·동 이름으로 받는다
        directory = await self._cafe_directory()
        allowed = sorted({t for c in directory for t in _region_tokens(c["address"])})
        parsed = await parse_query(query, allowed, self.settings)
        _rag_logger.debug(f"[질문 분해] 쿼리: {query!r}\n  {parsed}\n" + "=" * 70)

        # 2. 주소 필터 — 포함 지역 중 하나라도 주소에 있고(OR), 제외 지역은 하나도 없는 카페
        if parsed.wants_region and not parsed.regions:
            return {"results": [], "notice": NOTICE_REGION_NOT_FOUND}
        region_filtered = bool(parsed.regions or parsed.exclude_regions)
        candidates = _filter_by_region(directory, parsed)
        if region_filtered and not candidates:
            return {"results": [], "notice": NOTICE_REGION_NOT_FOUND}

        # 3-a. 지역 말고 다른 조건이 없으면 벡터 검색 없이 긍정 리뷰 수로 순위를 매긴다
        if not parsed.purpose:
            return {"results": await self._rank_without_purpose(candidates, top_n), "notice": None}

        # 3-b. 조건 문장으로 리뷰 벡터 검색 (지역을 걸렀으면 후보 카페로 제한)
        candidate_ids = [str(c["cafe_id"]) for c in candidates] if region_filtered else None
        top_cafes = await self._search_top_cafes(parsed.purpose, candidate_ids, top_n)
        if not top_cafes:
            return {"results": [], "notice": None}

        # 4. LLM 2차 호출 — 지역은 필터로 이미 반영했으므로 조건 문장만 넘긴다
        return {"results": await self._pick_with_llm(parsed.purpose, top_cafes), "notice": None}

    async def _cafe_directory(self) -> list[dict]:
        """승인된 카페의 주소·리뷰 수 목록 (TTL 캐시). 조회에 실패하면 이전 캐시라도 쓴다."""
        now = time.monotonic()
        if self._directory_loaded_at is None or now - self._directory_loaded_at > _DIRECTORY_TTL_SEC:
            try:
                self._directory = await asyncio.to_thread(fetch_cafe_directory, self.settings)
                self._directory_loaded_at = now
            except Exception as e:
                if self._directory_loaded_at is None:
                    raise
                logger.warning("카페 목록 갱신 실패, 이전 캐시 사용: %s", e)
        return self._directory

    async def _rank_without_purpose(self, candidates: list[dict], top_n: int) -> list[dict]:
        """조건 문장이 없을 때 — 긍정(GOOD) 리뷰 수, 같으면 전체 리뷰 수 순. 추천 문구는 대표 리뷰."""
        ranked = sorted(
            (c for c in candidates if c["review_count"] > 0),
            key=lambda c: (c["good_count"], c["review_count"]),
            reverse=True,
        )[:top_n]
        if not ranked:
            return []
        reviews = await asyncio.to_thread(
            fetch_representative_reviews, self.settings, [c["cafe_id"] for c in ranked]
        )
        return [
            {
                "cafeId":   c["cafe_id"],
                "cafeName": c["cafe_name"],
                "address":  c["address"],
                "snippet":  (reviews.get(c["cafe_id"]) or "")[:150],
                "score":    None,
            }
            for c in ranked
        ]

    async def _search_top_cafes(self, text: str, candidate_ids: list[str] | None, top_n: int) -> list[dict]:
        """조건 문장과 비슷한 리뷰를 모아 카페별 최고 점수 기준 상위 top_n 카페를 고른다."""
        # 카페 다양성 보장을 위해 필요한 만큼만 추가 조회
        # 캡: 1번째 카페 max 5, 2번째 max 4, …, 5번째 이후 max 1
        # 슬롯이 가득 찬 카페는 where 필터로 제외하고 부족분만 재조회
        target = top_n * 3  # 최종 목표 리뷰 수 (15)
        query_emb = await asyncio.to_thread(self._emb_fn.embed_query, text)
        total = self._col.count()

        cafe_order: list[str] = []
        cafe_counts: dict[str, int] = {}
        excluded: set[str] = set()
        seen: set[str] = set()
        sel_metas, sel_distances = [], []

        while len(sel_metas) < target:
            need = target - len(sel_metas)
            # 이미 본 리뷰(캡이 안 찬 카페의 리뷰)가 다시 상위에 오므로 그만큼 더 조회해 건너뛴다.
            # 건너뛰지 않으면 같은 리뷰가 중복으로 담긴다.
            batch = self._col.query(
                query_embeddings=[query_emb],
                n_results=min(need + len(seen), total),
                where=_where(candidate_ids, excluded),
            )
            added = 0
            for rid, meta, dist in zip(batch["ids"][0], batch["metadatas"][0], batch["distances"][0]):
                if rid in seen:
                    continue
                seen.add(rid)
                cid = meta["cafe_id"]
                if cid not in cafe_counts:
                    cafe_order.append(cid)
                    cafe_counts[cid] = 0
                cap = max(1, top_n - cafe_order.index(cid))  # 1등:5, 2등:4, …
                if cafe_counts[cid] < cap:
                    cafe_counts[cid] += 1
                    sel_metas.append(meta)
                    sel_distances.append(dist)
                    added += 1
                    if len(sel_metas) >= target:
                        break
                if cafe_counts[cid] >= cap:
                    excluded.add(cid)

            if added == 0:
                break  # 더 이상 추가 가능한 리뷰 없음

        # --- 벡터 검색 결과 로그 ---
        _log_retrieved_reviews(text, sel_metas, sel_distances)

        # 카페별 최고 유사도 점수로 그룹핑
        cafe_map: dict[str, dict] = {}
        for meta, dist in zip(sel_metas, sel_distances):
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

        # --- 최종 선별 카페 로그 (최대 5개) ---
        _log_top_cafes(text, top_cafes)
        return top_cafes

    async def _pick_with_llm(self, purpose: str, top_cafes: list[dict]) -> list[dict]:
        """후보 카페의 리뷰만 보여주고 조건에 맞는 카페와 추천 이유를 LLM 에게 고르게 한다."""
        # LLM 컨텍스트 — 리뷰만 보여준다. 이름·주소는 판단에 섞지 않고 응답 변환 때 채운다.
        context = "\n\n".join(
            f"[카페{i}] ID={c['cafe_id']}\n"
            f"리뷰: {c['review']}"
            for i, c in enumerate(top_cafes, 1)
        )
        user_msg = (
            f"사용자 질문: {purpose}\n\n"
            f"검색된 카페 정보:\n{context}\n\n"
            "위 카페 중 사용자 질문에 가장 잘 맞는 카페를 JSON으로 반환하세요."
        )

        try:
            raw = await call_llm(
                system_prompt=_SYSTEM_PROMPT,
                user_message=user_msg,
                model=self.settings.llm_model,
                base_url=self.settings.llm_base_url,
                api_key=self.settings.llm_api_key,
                timeout=self.settings.llm_timeout,
            )
            items = raw.get("results", [])
            if not isinstance(items, list):
                raise ValueError(f"results 필드가 리스트가 아님: {items!r}")
        except Exception as e:
            logger.warning("LLM 호출 실패, 검색 결과 직접 반환: %s", e)
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

        # LLM 응답 → PickBotResult 형태로 변환
        # 이름·주소는 LLM 에게 보여주지 않았으므로 검색 결과 메타데이터에서 cafeId 로 채운다.
        # 검색 결과에 없는 cafeId(LLM 이 지어낸 값)와 중복은 버린다.
        cafe_by_id = {c["cafe_id"]: c for c in top_cafes}
        output = []
        for item in items:
            try:
                cid = int(item["cafeId"])
            except (KeyError, ValueError, TypeError) as e:
                logger.warning("LLM 응답 항목 파싱 오류: %s — %r", e, item)
                continue
            cafe = cafe_by_id.pop(cid, None)
            if cafe is None:
                logger.warning("검색 결과에 없거나 중복된 cafeId 라 제외: %r", item)
                continue
            output.append({
                "cafeId":   cid,
                "cafeName": cafe["cafe_name"],
                "address":  cafe["address"],
                "snippet":  str(item.get("snippet", "")),
                "score":    round(cafe["score"], 4),
            })

        return output


# ---------------------------------------------------------------------------
# 지역 필터 헬퍼
# ---------------------------------------------------------------------------

def _region_tokens(address: str) -> list[str]:
    """주소에서 지역 단위(구·동)만 뽑는다. '서울시 마포구 연남동 48-37' → ['마포구', '연남동']

    첫 토큰(시·도)과 숫자가 들어간 토큰(번지)은 뺀다.
    """
    return [p for p in address.split()[1:] if not any(ch.isdigit() for ch in p)]


def _filter_by_region(directory: list[dict], parsed: ParsedQuery) -> list[dict]:
    """포함 지역 중 하나라도 주소에 있고(OR), 제외 지역은 하나도 없는 카페. 지역 조건이 없으면 전체."""
    include, exclude = set(parsed.regions), set(parsed.exclude_regions)
    result = []
    for cafe in directory:
        tokens = set(_region_tokens(cafe["address"]))
        if include and not tokens & include:
            continue
        if tokens & exclude:
            continue
        result.append(cafe)
    return result


def _where(candidate_ids: list[str] | None, excluded: set[str]) -> dict | None:
    """ChromaDB where 조건 — 후보 카페로 제한 + 리뷰 상한이 찬 카페 제외."""
    conds = []
    if candidate_ids is not None:
        conds.append({"cafe_id": {"$in": candidate_ids}})
    if excluded:
        conds.append({"cafe_id": {"$nin": sorted(excluded)}})
    if not conds:
        return None
    return conds[0] if len(conds) == 1 else {"$and": conds}


# ---------------------------------------------------------------------------
# 내부 로깅 헬퍼
# ---------------------------------------------------------------------------

def _log_retrieved_reviews(query: str, metas: list[dict], distances: list[float]) -> None:
    """벡터 검색으로 뽑힌 리뷰 목록을 rag_selection.log에 기록."""
    lines = [
        f"[벡터 검색 결과] 쿼리: {query!r}  |  검색 건수: {len(metas)}",
        "-" * 70,
    ]
    for i, (meta, dist) in enumerate(zip(metas, distances), 1):
        similarity = round(1.0 - dist, 4)
        review_preview = meta.get("review", "")[:80].replace("\n", " ")
        lines.append(
            f"  [{i:02d}] cafe_id={meta.get('cafe_id')}  sim={similarity:.4f}\n"
            f"        카페명: {meta.get('cafe_name')}  |  주소: {meta.get('address')}\n"
            f"        리뷰: {review_preview}{'...' if len(meta.get('review','')) > 80 else ''}"
        )
    lines.append("=" * 70)
    _rag_logger.debug("\n".join(lines))


def _log_top_cafes(query: str, top_cafes: list[dict]) -> None:
    """카페별 그룹핑 후 최종 선별된 카페 목록을 rag_selection.log에 기록."""
    lines = [
        f"[최종 선별 카페] 쿼리: {query!r}  |  선별 건수: {len(top_cafes)}",
        "-" * 70,
    ]
    for i, cafe in enumerate(top_cafes, 1):
        review_preview = cafe.get("review", "")[:80].replace("\n", " ")
        lines.append(
            f"  [{i}] cafe_id={cafe['cafe_id']}  score={cafe['score']:.4f}\n"
            f"      카페명: {cafe['cafe_name']}  |  주소: {cafe['address']}\n"
            f"      대표리뷰: {review_preview}{'...' if len(cafe.get('review','')) > 80 else ''}"
        )
    lines.append("=" * 70)
    _rag_logger.debug("\n".join(lines))
