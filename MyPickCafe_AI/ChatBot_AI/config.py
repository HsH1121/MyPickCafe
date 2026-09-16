from pathlib import Path

from pydantic_settings import BaseSettings

# .env 는 실행 위치와 무관하게 MyPickCafe_AI/.env 하나만 바라본다.
# (상대경로로 두면 CWD 기준이라 ChatBot_AI/ 에서 띄우느냐 루트에서 띄우느냐에 따라
#  다른 파일을 읽고, 설정이 조용히 기본값으로 되돌아간다.)
_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    # LLM (OpenAI 호환 /chat/completions) — base_url 은 /chat/completions 바로 앞까지
    llm_base_url:   str = "http://localhost:11434/v1"
    llm_api_key:    str = ""
    ollama_model:   str = "qwen2.5:14b"
    ollama_timeout: int = 60

    # 임베딩 (Ollama 네이티브 /api/embed) — LLM 과 다른 서버를 가리킬 수 있다
    embed_base_url: str = "http://localhost:11434"
    embed_model:    str = "bge-m3"

    # PostgreSQL (chatbot RAG 인덱싱용) — .env 파일에서 주입
    db_host:     str = "localhost"
    db_port:     int = 5432
    db_name:     str = "mypickcafe"
    db_user:     str = "mypickcafe"
    db_password: str = ""

    # ChromaDB 저장 경로
    chroma_path: str = "./chroma_db"

    class Config:
        env_file = _ENV_FILE
        env_file_encoding = "utf-8"
        # 공용 .env 를 여러 패키지가 나눠 쓰므로, 이 클래스가 선언하지 않은
        # 키(다른 패키지용 설정)는 무시한다. 없으면 extra_forbidden 으로 죽는다.
        extra = "ignore"
