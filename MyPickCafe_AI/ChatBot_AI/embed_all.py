"""
Oracle DB의 리뷰 전체를 ChromaDB에 임베딩합니다.
FastAPI 서버 없이 독립 실행 가능.

실행 전 확인사항:
  1. Oracle DB 실행 중 (SQL을 통해 리뷰 데이터 INSERT 완료)
  2. Ollama 실행 중 (ollama serve)
  3. nomic-embed-text 모델 다운로드 완료 (ollama pull nomic-embed-text)

사용법:
  python embed_all.py          # 신규 리뷰만 추가
  python embed_all.py --reset  # ChromaDB 초기화 후 전체 재임베딩
"""

# True: ChromaDB 전체 초기화 후 재임베딩 / False: 신규 리뷰만 추가
RESET_CHROMA = False

import sys
import argparse
import logging

import ollama

from config import Settings
from chatbot_rag import CafeRAG

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


def main() -> None:
    parser = argparse.ArgumentParser(description="카페 리뷰 전체 임베딩")
    parser.add_argument(
        "--reset",
        action="store_true",
        help="ChromaDB를 초기화하고 전체 재임베딩",
    )
    args = parser.parse_args()

    # Ollama 연결 확인
    print("  Ollama 연결 확인 중...")
    try:
        ollama.list()
    except Exception:
        print("❌ Ollama가 실행되지 않았습니다.")
        print("   'ollama serve' 명령어 또는 Ollama 앱을 실행한 뒤 다시 시도하세요.")
        sys.exit(1)

    settings = Settings()
    rag = CafeRAG(settings)

    before = rag.indexed_count

    if (RESET_CHROMA or args.reset) and before > 0:
        print(f"  기존 인덱스 {before:,}건 초기화 중...")
        rag._client.delete_collection(rag._col.name)
        rag._col = rag._client.get_or_create_collection(
            name=rag._col.name,
            embedding_function=rag._emb_fn,
            metadata={"hnsw:space": "cosine"},
        )
        print("  초기화 완료\n")

    print(f"  현재 인덱스: {rag.indexed_count:,}건")
    print("  Oracle DB → ChromaDB 임베딩 시작... (시간이 걸릴 수 있습니다)\n")

    try:
        added = rag.index_from_db()
    except Exception as e:
        print(f"❌ 임베딩 실패: {e}")
        sys.exit(1)

    after = rag.indexed_count
    if added == 0:
        print("  새로 추가된 리뷰가 없습니다. (전체 인덱스 유지)")
    else:
        print(f"  임베딩 완료: {added:,}건 추가 -> 총 {after:,}건")


if __name__ == "__main__":
    main()
