from pathlib import Path

from pydantic import field_validator
from pydantic_settings import BaseSettings

# MyPickCafe_AI/ — 실행 위치와 무관하게 파일 경로를 풀 기준 디렉터리
_AI_ROOT = Path(__file__).resolve().parent.parent

# .env 는 실행 위치와 무관하게 MyPickCafe_AI/.env 하나만 바라본다.
# (상대경로로 두면 CWD 기준이라 ChatBot_AI/ 에서 띄우느냐 루트에서 띄우느냐에 따라
#  다른 파일을 읽고, 설정이 조용히 기본값으로 되돌아간다.)
_ENV_FILE = _AI_ROOT / ".env"


class Settings(BaseSettings):
    # LLM (OpenAI 호환 /chat/completions) — base_url 은 /chat/completions 바로 앞까지
    # 호스트를 localhost 가 아닌 127.0.0.1 로 둔다. Windows 에서 localhost 는 IPv6(::1) 부터
    # 시도하는데 Ollama 는 IPv4 에서만 대기해, 새 연결마다 약 2.2s 가 붙는다.
    llm_base_url:   str = "http://127.0.0.1:11434/v1"
    llm_api_key:    str = ""
    llm_model:      str = "qwen2.5:14b"
    llm_timeout:    int = 60

    # 임베딩 (Ollama 네이티브 /api/embed) — LLM 과 다른 서버를 가리킬 수 있다
    embed_base_url: str = "http://127.0.0.1:11434"
    embed_model:    str = "bge-m3"
    # 소량 임베딩(검색 쿼리·리뷰 1건 upsert)을 CPU 로 돌릴지. 로컬에서 LLM 과 GPU 를
    # 나눠 쓸 때 요청마다 모델을 내렸다 올리는 비용(수 초)을 없앤다.
    # LLM 이 원격이거나 VRAM 이 넉넉한 환경이면 false 로 둬도 된다.
    embed_small_batches_on_cpu: bool = True

    # PostgreSQL (chatbot RAG 인덱싱용) — .env 파일에서 주입
    db_host:     str = "localhost"
    db_port:     int = 5432
    db_name:     str = "mypickcafe"
    db_user:     str = "mypickcafe"
    db_password: str = ""

    # ChromaDB 저장 경로 — 상대경로는 실행 위치가 아니라 MyPickCafe_AI/ 기준으로 푼다.
    # 실행 위치 기준이면 ChatBot_AI/ 에서 돌린 embed_all.py 와 MyPickCafe_AI/ 에서
    # 띄운 app.py 가 서로 다른 인덱스를 보고, app.py 가 전체 재인덱싱을 한다.
    chroma_path: str = "./chroma_db"

    @field_validator("chroma_path")
    @classmethod
    def _resolve_chroma_path(cls, v: str) -> str:
        p = Path(v)
        return str(p if p.is_absolute() else (_AI_ROOT / p).resolve())

    class Config:
        env_file = _ENV_FILE
        env_file_encoding = "utf-8"
        # 공용 .env 를 여러 패키지가 나눠 쓰므로, 이 클래스가 선언하지 않은
        # 키(다른 패키지용 설정)는 무시한다. 없으면 extra_forbidden 으로 죽는다.
        extra = "ignore"
