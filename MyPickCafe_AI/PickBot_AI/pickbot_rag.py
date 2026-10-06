"""
카페 추천 RAG 파이프라인

1. index_from_db()  — PostgreSQL 리뷰를 ChromaDB에 임베딩+저장
2. recommend()      — 질문 분해(LLM 1차) → 주소로 지역 필터 → 조건별 리뷰 유사도 검색·커버리지 순위 → LLM 2차 추천

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
import numpy as np
from chromadb import Documents, EmbeddingFunction, Embeddings
import httpx

from config import Settings
from pickbot_db import fetch_cafe_directory, fetch_representative_reviews, fetch_reviews_for_index
from llm_client import call_llm
from pick_trace import NO_TRACE, PickTrace, format_attempts
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

# 응답 notice 값 — 허용 목록의 지역으로 걸렀더니 남는 카페가 없음 (목록 밖 지역은 무시하고 전체 검색)
NOTICE_REGION_NOT_FOUND = "REGION_NOT_FOUND"
# 응답 notice 값 — 서울 밖 지명을 말했다. 그 지역은 거르지 않고 찾은 결과와 함께 "서울 내 카페만" 안내를 한다.
NOTICE_OUTSIDE_SEOUL = "OUTSIDE_SEOUL"

# LLM 2차 호출(추천 선택)의 출력 토큰 한도. 추론 모델은 답 전에 추론 토큰을 쓰고 추천 5곳의
# 이유 문장까지 쓰므로 공용 기본값 1000 에 자주 걸렸다(실측: deepseek 7/10, glm-5p3-flash 1/10).
# 걸리면 content 가 비어 재시도로 지연이 몇 배가 되고 결국 검색 결과만 반환된다.
# 한도는 상한일 뿐이라 실제로 생성한 만큼만 과금된다.
_PICK_MAX_TOKENS = 3000

# LLM 2차 호출의 추론량은 settings.llm_reasoning_effort 로 받는다(배포 환경 "low").
# 카페 5곳 × 요구사항을 판정하는 프롬프트에서 glm-5p3-flash 가 추론만 하다
# 한도를 다 썼다(실측: 29.8초, 출력 3000 전부 추론, 빈 응답 → 20초 타임아웃·재시도로 실패).
# "low" 로 같은 입력이 3.4초·출력 394토큰에 정상 판정됐다. GLM-5.3 은 추론을 끌 수 없다("none" 은 400).
# Ollama qwen2.5 같은 비추론 모델은 값을 보내면 400("does not support thinking")이므로 그때는 비워 둔다.

# 카페 목록(주소·리뷰 수) 캐시 유지 시간. 요청마다 DB 를 조회하지 않기 위함.
_DIRECTORY_TTL_SEC = 300

_SYSTEM_PROMPT = """당신은 카페 리뷰가 사용자 요구사항을 충족하는지 판정하는 AI입니다.
카페 이름과 위치는 주어지지 않습니다. 추측해서 쓰지 마세요.
반드시 JSON 객체 하나만 반환하세요. 설명 텍스트 절대 금지.

## 할 일
1. 요구사항 목록이 주어집니다. 이 목록을 그대로 씁니다. 다시 나누거나 합치거나 바꾸지 마세요.
2. 검색된 카페마다 리뷰가 각 요구사항을 충족하는지 판정해 matched 와 missing 에 요구사항 문구 그대로 나눠 넣습니다.
   모든 요구사항이 matched 와 missing 중 정확히 한 곳에 들어가야 합니다.
   - 리뷰 옆 "검색 근거"는 검색 유사도로 붙인 표시일 뿐입니다. 리뷰 내용만 보고 판정하세요.
   - 글자가 달라도 뜻이 같으면 충족입니다. ("카공하기 좋아요" → "노트북 하기 좋음" 충족)
   - 리뷰에 언급이 없으면 미충족입니다. 추측하지 마세요.
   - 반대 내용("주차 불가", "시끄러워요")이면 미충족입니다.
3. 카페마다 snippet 을 씁니다.
   - matched 에 있는 요구사항만 근거로, 리뷰 내용에 기반한 추천 이유 1~2문장.
   - missing 에 있는 요구사항은 충족한 것처럼 쓰지 마세요.
   - matched 가 비어 있으면 리뷰에서 드러나는 장점을 1문장으로 씁니다.
   - 문장은 "~다는 평이 많습니다."로 끝냅니다("평이 있습니다"로 바꾸지 않습니다). 2문장이면 마지막 문장만 이렇게 끝내고 앞 문장은 "~합니다"로 씁니다.
   - "~카페입니다", "~리뷰입니다", "~리뷰가 있습니다", "~후기가 많습니다", "~라고 합니다"처럼 쓰지 마세요.
     나쁜 예: "디저트가 맛있다는 리뷰가 있습니다." / "주차가 편한 카페입니다." / "디저트가 맛있다고 합니다."
     좋은 예: "디저트가 맛있고 주차 공간이 넉넉해 방문하기 편하다는 평이 많습니다."
4. 검색된 카페를 하나도 빠뜨리지 말고 모두 cafes 에 넣습니다.

## 형식
{"cafes": [{"cafeId": <정수>, "matched": ["<요구사항>"], "missing": ["<요구사항>"], "snippet": "<추천 이유>"}]}"""


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
        PICKBOT_TRACE=true(개발 환경)면 단계별 시간·결과를 logs/pickbot_trace.log 와 콘솔에 남긴다.
        """
        trace = PickTrace(query, self.settings.pickbot_trace)
        try:
            result = await self._recommend(query, top_n, trace)
        except BaseException as e:
            trace.finish(f"오류 {type(e).__name__}: {e}")
            raise
        notice = f", 안내 {result['notice']}" if result["notice"] else ""
        trace.finish(f"{len(result['results'])}곳 반환{notice}")
        return result

    async def _recommend(self, query: str, top_n: int, trace: PickTrace) -> dict:
        if self._col.count() == 0:
            logger.warning("ChromaDB가 비어 있습니다. 먼저 /pickbot/reindex를 호출하세요.")
            trace.step("인덱스", None, "ChromaDB 가 비어 있음")
            return {"results": [], "notice": None}

        # 1. 질문 분해 — 지역은 카페 주소에 실제로 있는 구·동 이름으로 받는다
        started = trace.now()
        directory = await self._cafe_directory()
        allowed = sorted({t for c in directory for t in _region_tokens(c["address"])})
        trace.step("카페 목록", started, f"{len(directory)}곳, 허용 지역 {len(allowed)}개")
        parsed = await parse_query(query, allowed, self.settings, trace)
        _rag_logger.debug(f"[질문 분해] 쿼리: {query!r}\n  {parsed}\n" + "=" * 70)

        # 2. 주소 필터 — 포함 지역 중 하나라도 주소에 있고(OR), 제외 지역은 하나도 없는 카페
        # 허용 목록 밖의 지역(unmatched_regions)은 거르지 않고 무시한다. 모든 카페가 서울에 있어
        # 모델이 "서울"이나 "제주 말차"의 제주를 지역으로 잘못 뽑아도 추천이 막히지 않게 하기 위함이다.
        started = trace.now()
        region_filtered = bool(parsed.regions or parsed.exclude_regions)
        candidates = _filter_by_region(directory, parsed)
        # 서울 밖 지명은 거르지 않고, 찾은 결과와 함께 "서울 내 카페만" 안내를 한다
        notice = NOTICE_OUTSIDE_SEOUL if parsed.outside_regions else None
        outside = f" (서울 밖 {parsed.outside_regions} → 안내 {notice})" if notice else ""
        if region_filtered:
            trace.step("③ 주소 필터", started,
                       f"지역={parsed.regions} 제외={parsed.exclude_regions} → 후보 {len(candidates)}곳{outside}")
        else:
            others = [r for r in parsed.unmatched_regions if r not in parsed.outside_regions]
            ignored = f" (목록 밖 지역 {others} 무시)" if others else ""
            trace.step("③ 주소 필터", started, f"지역 조건 없음 → 전체 {len(candidates)}곳{ignored}{outside}")
        if region_filtered and not candidates:
            return {"results": [], "notice": NOTICE_REGION_NOT_FOUND}

        # 3-a. 지역 말고 다른 조건이 없으면 벡터 검색 없이 긍정 리뷰 수로 순위를 매긴다
        conditions = parsed.condition_list()
        if not conditions:
            trace.step("④ 벡터 검색", None, "건너뜀 (조건 문장 없음)")
            trace.step("⑤ 판정 LLM", None, "건너뜀 → 긍정 리뷰 수 순위")
            return {"results": await self._rank_with_only_region(candidates, top_n, trace), "notice": notice}

        # 3-b. 조건별 리뷰 벡터 검색 → 카페별 조건 커버리지 순위 (지역을 걸렀으면 후보 카페로 제한)
        candidate_ids = [str(c["cafe_id"]) for c in candidates] if region_filtered else None
        top_cafes = await self._rag_cafes(conditions, candidate_ids, trace=trace)
        if not top_cafes:
            trace.step("⑤ 판정 LLM", None, "건너뜀 (검색 결과 없음)")
            return {"results": [], "notice": notice}

        # 4. LLM 2차 호출 — 지역은 필터로 이미 반영했으므로 조건 목록을 요구사항으로 그대로 넘긴다
        return {"results": await self._pick_with_llm(conditions, top_cafes, trace), "notice": notice}

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

    async def _rank_with_only_region(self, candidates: list[dict], top_n: int,
                                     trace: PickTrace = NO_TRACE) -> list[dict]:
        """조건 문장이 없을 때 — 긍정(GOOD) 리뷰 수, 같으면 전체 리뷰 수 순. 추천 문구는 대표 리뷰."""
        started = trace.now()
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
        trace.step("   긍정 리뷰 순위", started, ", ".join(
            f"[{c['cafe_id']}] {c['cafe_name']} (긍정 {c['good_count']}/{c['review_count']})" for c in ranked))
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

    async def _rag_cafes(self, conditions: list[str], candidate_ids: list[str] | None,
                         trace: PickTrace = NO_TRACE) -> list[dict]:
        """조건별 검색 → 후보 리뷰 × 모든 조건 유사도 → 카페별 커버리지 순위. 판정 LLM 에 넘길 상위 카페를 돌려준다.

        1. 조건 목록을 한 번에 임베딩한다(합치지 않고 조건마다 벡터 하나).
        2. 조건마다 상위 리뷰를 찾는다(카페당 상한). ChromaDB 는 모든 조건을 한 번에 묻고 저장된 리뷰 임베딩도 같이 받는다.
        3. 조건별 결과를 합쳐 중복을 없애고, 후보 리뷰 전부를 모든 조건과 다시 비교한다(리뷰 재임베딩 없음).
        4. 카페별로 조건마다 가장 잘 맞는 리뷰(근거)를 고르고, 리뷰 하나로 전부 > 여러 리뷰로 전부 > 일부, 같으면 유사도 순.
        """
        s = self.settings
        started = trace.now()
        cond_embs = await asyncio.to_thread(self._emb_fn, conditions)
        trace.step("④ 조건 임베딩", started, f"{len(conditions)}개 한 번에 {conditions}")

        started = trace.now()
        pool, per_condition, queries = await asyncio.to_thread(self._search_by_condition, cond_embs, candidate_ids)
        if not pool:
            trace.step("   벡터 검색", started, "후보 리뷰 없음")
            return []
        ranked = rank_cafes_by_coverage(
            conditions, cond_embs, pool, s.pickbot_condition_threshold, s.pickbot_judge_reviews_per_cafe)
        top_cafes = ranked[:s.pickbot_judge_max_cafes]

        trace.step("   벡터 검색", started,
                   f"조건 {len(conditions)}개 × 최대 {s.pickbot_reviews_per_condition}개(카페당 {s.pickbot_search_reviews_per_cafe}개) "
                   f"→ 후보 리뷰 {len(pool)}건(중복 제거), 카페 {len(ranked)}곳, ChromaDB 조회 {queries}번")
        for cond, ids in zip(conditions, per_condition):
            trace.note(f"{cond!r}: 리뷰 {len(ids)}개, 카페 {len({pool[rid]['meta']['cafe_id'] for rid in ids})}곳")
        trace.step("   커버리지 순위", None,
                   f"임계값 {s.pickbot_condition_threshold} → 상위 {len(top_cafes)}곳을 판정 LLM 에 (전체 {len(ranked)}곳)")
        for c in top_cafes:
            evidence = ", ".join(f"{e['condition']} {e['score']:.3f}" for e in c["evidence"])
            trace.note(f"[{c['cafe_id']}] {c['cafe_name']} {c['covered']}/{len(conditions)} {_TIER_LABEL[c['tier']]} "
                       f"유사도 {c['score']:.3f} | {evidence} | {c['review'][:40]}")

        _log_condition_search(conditions, pool, per_condition, top_cafes)
        return top_cafes

    def _search_by_condition(self, cond_embs: list, candidate_ids: list[str] | None
                             ) -> tuple[dict[str, dict], list[list[str]], int]:
        """조건마다 상위 리뷰를 카페당 상한까지 모은다. → (후보 리뷰 {id: {meta, emb}}, 조건별 리뷰 id 목록, 조회 횟수)

        모든 조건을 ChromaDB 한 번의 조회로 묻는다. 카페당 상한 때문에 버려질 몫까지 넉넉히(_SEARCH_OVERFETCH 배) 받고,
        리뷰가 많은 카페 몇 곳이 상위를 독차지해 그래도 모자란 조건만, 상한이 찬 카페를 빼고 다시 묻는다.
        """
        s = self.settings
        per_cond, cap = s.pickbot_reviews_per_condition, s.pickbot_search_reviews_per_cafe
        total = self._col.count()
        include = ["metadatas", "distances", "embeddings"]
        res = self._col.query(query_embeddings=cond_embs, n_results=min(per_cond * _SEARCH_OVERFETCH, total),
                              where=_where(candidate_ids, set()), include=include)
        queries = 1

        pool: dict[str, dict] = {}
        per_condition: list[list[str]] = []
        for j, emb in enumerate(cond_embs):
            picked: list[str] = []
            per_cafe: dict[str, int] = {}
            ids, metas, embs = res["ids"][j], res["metadatas"][j], res["embeddings"][j]
            while True:
                before = len(picked)
                for rid, meta, review_emb in zip(ids, metas, embs):
                    cafe_id = meta["cafe_id"]
                    if rid in picked or per_cafe.get(cafe_id, 0) >= cap:
                        continue
                    per_cafe[cafe_id] = per_cafe.get(cafe_id, 0) + 1
                    picked.append(rid)
                    pool.setdefault(rid, {"meta": meta, "emb": review_emb})
                    if len(picked) >= per_cond:
                        break
                full = {c for c, n in per_cafe.items() if n >= cap}
                if (len(picked) >= per_cond or len(ids) < min(per_cond * _SEARCH_OVERFETCH, total)
                        or not full or len(picked) == before):
                    break  # 다 찼거나, 조건에 맞는 리뷰를 이미 다 받았거나, 다시 물어도 새로 고를 리뷰가 없음
                # 상한이 찬 카페를 빼고 다시 묻는다. 이미 고른 리뷰는 그 카페들 것이라 다시 나오지 않는다.
                more = self._col.query(query_embeddings=[emb], n_results=min(per_cond * _SEARCH_OVERFETCH, total),
                                       where=_where(candidate_ids, full), include=include)
                queries += 1
                ids, metas, embs = more["ids"][0], more["metadatas"][0], more["embeddings"][0]
                if not ids:
                    break
            per_condition.append(picked)
        return pool, per_condition, queries

    async def _pick_with_llm(self, conditions: list[str], top_cafes: list[dict],
                             trace: PickTrace = NO_TRACE) -> list[dict]:
        """후보 카페의 대표 리뷰만 보여주고 요구사항(조건 목록 그대로)을 충족한 카페와 추천 이유를 LLM 에게 고르게 한다."""
        started, attempts = trace.now(), []
        try:
            raw = await call_llm(
                system_prompt=_SYSTEM_PROMPT,
                user_message=build_pick_user_message(conditions, top_cafes),
                model=self.settings.llm_model,
                base_url=self.settings.llm_base_url,
                api_key=self.settings.llm_api_key,
                timeout=self.settings.llm_timeout,
                max_tokens=_PICK_MAX_TOKENS,
                reasoning_effort=self.settings.llm_reasoning_effort or None,
                attempt_log=attempts,
            )
            picks, stats = validate_llm_response_and_select_cafes(raw, top_cafes, conditions)
        except Exception as e:
            logger.warning("LLM 호출 실패, 검색 결과 직접 반환: %s", e)
            trace.step("⑤ 판정 LLM", started,
                       f"실패 {type(e).__name__} ({format_attempts(attempts)}) → 검색 결과 그대로 반환")
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

        _rag_logger.debug(
            f"[요구사항 판정] 조건: {conditions}\n"
            f"  요구사항: {stats['requirements']}\n"
            f"  모두 충족 {stats['full_match']}곳"
            f"{' → 일부 충족 후보 중 검색 순위 1위만 반환' if stats['fallback_top1'] else ''}"
            f"{' → 충족 조건이 있는 후보가 없어 빈 목록 반환' if not picks else ''}"
            f", 무시한 응답 항목 {stats['ignored']}개\n"
            + "\n".join(f"  [{cid}] 충족={v['matched']} 누락={v['missing']}" for cid, v in stats["verdicts"].items())
            + "\n" + "=" * 70
        )
        retry = format_attempts(attempts)
        how = ("모두 충족한 카페" if stats["full_match"] else
               "모두 충족 없음 → 일부 충족 중 순위 1위" if picks else "충족 조건이 있는 카페 없음 → 빈 목록")
        trace.step("⑤ 판정 LLM", started, f"요구사항={stats['requirements']} 모두 충족 {stats['full_match']}곳 "
                   f"→ {how} {len(picks)}곳 반환" + (f"  ({retry})" if retry else ""))
        names = {c["cafe_id"]: c["cafe_name"] for c in top_cafes}
        picked = {p["cafeId"] for p in picks}
        for cid, v in stats["verdicts"].items():
            trace.note(f"{'✓' if cid in picked else ' '} [{cid}] {names.get(cid, '')} "
                       f"충족={v['matched']} 누락={v['missing']}")
        return picks


# ---------------------------------------------------------------------------
# LLM 2차 호출 메시지
# ---------------------------------------------------------------------------

def build_pick_user_message(conditions: list[str] | str, top_cafes: list[dict]) -> str:
    """LLM 2차 호출의 사용자 메시지. 테스트(test_pick_llm.py)도 같은 함수를 쓴다.

    요구사항은 1차 LLM 이 나눈 조건 목록을 그대로 준다. 카페마다 조건별 대표 리뷰(rank_cafes_by_coverage 의
    reviews)를 검색 근거 조건과 함께 보여준다. 이름·주소는 판단에 섞지 않고 응답 변환 때 검색 결과에서 채운다.
    """
    if isinstance(conditions, str):
        conditions = [conditions]

    def cafe_block(i: int, c: dict) -> str:
        reviews = c.get("reviews") or [{"text": c["review"], "conditions": []}]
        lines = []
        for k, r in enumerate(reviews, 1):
            basis = f" (검색 근거: {', '.join(r['conditions'])})" if r["conditions"] else ""
            lines.append(f"리뷰{k}{basis}: {r['text']}")
        return f"[카페{i}] ID={c['cafe_id']}\n" + "\n".join(lines)

    context = "\n\n".join(cafe_block(i, c) for i, c in enumerate(top_cafes, 1))
    return (
        f"요구사항 (이 목록 그대로 판정): {json.dumps(conditions, ensure_ascii=False)}\n\n"
        f"검색된 카페 정보:\n{context}\n\n"
        "위 카페 각각의 리뷰가 요구사항을 하나씩 충족하는지 판정해 JSON으로 반환하세요."
    )


def validate_llm_response_and_select_cafes(raw: dict, top_cafes: list[dict],
                                           requirements: list[str] | None = None) -> tuple[list[dict], dict]:
    """LLM 요구사항 판정으로 반환할 카페를 고른다. 테스트(test_pick_llm.py)도 같은 함수를 쓴다.

    - 요구사항을 하나도 빠짐없이 충족(missing 이 빈 배열)한 카페를 전부, 검색 순위(top_cafes 순서)대로 반환한다.
    - 모두 충족한 카페가 없으면 일부 조건을 충족한 후보 중 검색 순위 1위만 반환한다.
      이때 추천 이유는 LLM 이 충족한 조건만 근거로 쓴 snippet 이다.
    - 조건을 하나라도 충족한 후보가 없으면 빈 목록을 반환한다.
    - 후보에 없는 cafeId, 중복, 형식이 틀린 항목은 무시하고, 판정이 없는 후보는 미충족으로 본다.
    - missing 이 없으면 requirements 에서 matched 를 뺀 것을 missing 으로 본다.
    - top_cafes 는 검색 순위(커버리지·유사도) 순이고 비어 있지 않아야 한다.
    - requirements 는 판정 LLM 에 넘긴 요구사항 목록이다. 없으면(예전 형식) 응답의 requirements 를 쓴다.
    """
    cafes = raw.get("cafes")
    if not isinstance(cafes, list):
        raise ValueError(f"cafes 필드가 리스트가 아님: {cafes!r}")
    if requirements is None:
        requirements = [r for r in (raw.get("requirements") or []) if isinstance(r, str)]

    candidate_ids = {c["cafe_id"] for c in top_cafes}
    verdicts: dict[int, dict] = {}
    ignored = 0
    for item in cafes:
        try:
            cid = int(item["cafeId"])
        except (KeyError, ValueError, TypeError):
            ignored += 1
            continue
        if cid not in candidate_ids or cid in verdicts:
            ignored += 1
            continue
        # 모두 충족한 카페의 missing 을 빈 배열 대신 생략(null)하는 응답이 있다(실측 7곳 중 5곳).
        # 요구사항 목록을 알 때는 목록에서 matched 를 빼서 채운다.
        if requirements and not isinstance(item.get("missing"), list) and isinstance(item.get("matched"), list):
            item = {**item, "missing": [r for r in requirements if r not in item["matched"]]}
        verdicts[cid] = item

    def all_met(verdict: dict) -> bool:
        missing = verdict.get("missing")
        return isinstance(missing, list) and not missing

    def any_met(verdict: dict) -> bool:
        matched = verdict.get("matched")
        return isinstance(matched, list) and any(
            isinstance(condition, str) and condition.strip() for condition in matched
        )

    matched_cafes = [
        c for c in top_cafes
        if c["cafe_id"] in verdicts and any_met(verdicts[c["cafe_id"]])
    ]
    full = [c for c in matched_cafes if all_met(verdicts[c["cafe_id"]])]
    chosen = full or matched_cafes[:1]

    picks = []
    for c in chosen:
        snippet = verdicts.get(c["cafe_id"], {}).get("snippet")
        if not isinstance(snippet, str) or not snippet.strip():
            snippet = c["review"][:150]
        picks.append({
            "cafeId":   c["cafe_id"],
            "cafeName": c["cafe_name"],
            "address":  c["address"],
            "snippet":  snippet.strip(),
            "score":    round(c["score"], 4),
        })
    stats = {
        "requirements":  requirements,
        "full_match":    len(full),
        "fallback_top1": bool(chosen) and not full,
        "ignored":       ignored,
        "verdicts":      {cid: {"matched": v.get("matched"), "missing": v.get("missing")} for cid, v in verdicts.items()},
    }
    return picks, stats


# ---------------------------------------------------------------------------
# 조건별 검색 — 커버리지 순위
# ---------------------------------------------------------------------------

# 카페당 상한 때문에 버려질 몫까지 한 번에 받기 위한 배수. 조건당 21개면 63개를 받는다.
_SEARCH_OVERFETCH = 3

_TIER_SINGLE, _TIER_MULTI, _TIER_PARTIAL = 2, 1, 0
_TIER_LABEL = {_TIER_SINGLE: "리뷰 하나로 전부", _TIER_MULTI: "여러 리뷰로 전부", _TIER_PARTIAL: "일부"}


def _normalize(rows) -> np.ndarray:
    m = np.asarray(rows, dtype=np.float32)
    norms = np.linalg.norm(m, axis=1, keepdims=True)
    return m / np.where(norms == 0, 1, norms)


def rank_cafes_by_coverage(conditions: list[str], cond_embs, pool: dict[str, dict],
                           threshold: float, reviews_per_cafe: int) -> list[dict]:
    """후보 리뷰 전부 × 조건 전부의 코사인 유사도로 카페별 조건 커버리지를 매기고 순위대로 돌려준다.

    pool: {리뷰 id: {"meta": ChromaDB 메타데이터, "emb": 저장된 리뷰 임베딩}} — 조건별 검색 결과를 합친 것
    - 리뷰는 유사도가 threshold 이상인 조건을 모두 충족한 후보다(리뷰 하나가 여러 조건 가능).
    - 카페는 리뷰들이 충족한 조건의 합집합만큼 커버한다(리뷰A P1+P2, 리뷰B P3 → 3/3).
    - 조건마다 그 카페에서 가장 잘 맞는 리뷰와 점수를 근거(evidence)로 보관한다.
    - 순위: 리뷰 하나로 전부 > 여러 리뷰로 전부 > 일부(커버한 조건 수 많은 순), 같으면 조건별 최고 유사도의 평균.
    - reviews 는 판정 LLM 에 보여줄 대표 리뷰(reviews_per_cafe 개 이하)다. 조건을 새로 커버하는 근거 리뷰부터
      고르고, 자리가 남으면(조건 1개일 때 등) 그 카페의 다른 후보 리뷰를 유사도 순으로 채운다.
    """
    ids = list(pool)
    sims = _normalize([pool[rid]["emb"] for rid in ids]) @ _normalize(cond_embs).T  # (리뷰 수, 조건 수)

    rows_by_cafe: dict[str, list[int]] = {}
    for i, rid in enumerate(ids):
        rows_by_cafe.setdefault(pool[rid]["meta"]["cafe_id"], []).append(i)

    cafes = []
    for cafe_id, rows in rows_by_cafe.items():
        sub = sims[rows]
        best_row = sub.argmax(axis=0)
        best = sub.max(axis=0)
        met = sub >= threshold
        covered = int((best >= threshold).sum())
        if met.all(axis=1).any():
            tier = _TIER_SINGLE
        elif covered == len(conditions):
            tier = _TIER_MULTI
        else:
            tier = _TIER_PARTIAL
        meta = pool[ids[rows[0]]]["meta"]
        reviews = [{"text": pool[ids[rows[r]]]["meta"]["review"],
                    "conditions": [conditions[j] for j in range(len(conditions)) if met[r, j]]}
                   for r in _pick_judge_reviews(sub, met, reviews_per_cafe)]
        cafes.append({
            "cafe_id":   int(cafe_id),
            "cafe_name": meta["cafe_name"],
            "address":   meta["address"],
            "review":    reviews[0]["text"],
            "reviews":   reviews,
            "score":     float(best.mean()),
            "covered":   covered,
            "tier":      tier,
            "evidence":  [{"condition": conditions[j], "review": pool[ids[rows[best_row[j]]]]["meta"]["review"],
                           "score": float(best[j])} for j in range(len(conditions))],
        })
    cafes.sort(key=lambda c: (c["tier"], c["covered"], c["score"]), reverse=True)
    return cafes


def _pick_judge_reviews(sub: np.ndarray, met: np.ndarray, limit: int) -> list[int]:
    """카페의 후보 리뷰 중 판정 LLM 에 보여줄 것을 limit 개 이하로 고른다. (sub 의 행 번호 목록)

    아직 커버하지 않은 조건을 가장 많이 새로 충족하는 리뷰부터, 같으면 조건별 유사도 최댓값 순.
    새로 커버할 조건이 없으면 남은 리뷰를 유사도 순으로 채운다.
    """
    chosen: list[int] = []
    covered = np.zeros(sub.shape[1], dtype=bool)
    remaining = list(range(sub.shape[0]))
    while remaining and len(chosen) < limit:
        best = max(remaining, key=lambda r: (int((met[r] & ~covered).sum()), float(sub[r].max())))
        chosen.append(best)
        covered |= met[best]
        remaining.remove(best)
    return chosen


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

def _log_condition_search(conditions: list[str], pool: dict[str, dict], per_condition: list[list[str]],
                          top_cafes: list[dict]) -> None:
    """조건별 검색 결과와 커버리지 순위로 고른 카페를 rag_selection.log 에 기록."""
    lines = [f"[조건별 검색] 조건: {conditions}  |  후보 리뷰 {len(pool)}건(중복 제거)", "-" * 70]
    for cond, ids in zip(conditions, per_condition):
        lines.append(f"  조건 {cond!r}: {len(ids)}건")
        for rid in ids:
            meta = pool[rid]["meta"]
            preview = meta.get("review", "")[:80].replace("\n", " ")
            lines.append(f"    cafe_id={meta.get('cafe_id')}  {meta.get('cafe_name')}  |  {preview}")
    lines.append("-" * 70)
    lines.append(f"[커버리지 순위] 판정 LLM 에 넘긴 카페 {len(top_cafes)}곳")
    for i, c in enumerate(top_cafes, 1):
        lines.append(f"  [{i}] cafe_id={c['cafe_id']}  {c['cafe_name']}  {c['covered']}/{len(conditions)} "
                     f"{_TIER_LABEL[c['tier']]}  score={c['score']:.4f}")
        for e in c["evidence"]:
            lines.append(f"      {e['condition']} {e['score']:.4f} | {e['review'][:80]}")
    lines.append("=" * 70)
    _rag_logger.debug("\n".join(lines))
