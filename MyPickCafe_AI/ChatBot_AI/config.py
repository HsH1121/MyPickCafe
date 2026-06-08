from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    model_api_url:  str = "http://localhost:11434"
    ollama_model:   str = "qwen2.5:14b"
    ollama_timeout: int = 60
    embed_model:    str = "bge-m3"

    # Oracle DB (chatbot RAG 인덱싱용) — .env 파일에서 주입
    db_user:     str = "HsH"
    db_password: str = ""
    db_dsn:      str = "localhost:1521/XE"

    # ChromaDB 저장 경로
    chroma_path: str = "./chroma_db"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
