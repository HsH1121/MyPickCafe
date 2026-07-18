from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    model_api_url:  str = "http://localhost:11434"
    ollama_model:   str = "qwen2.5:14b"
    ollama_timeout: int = 60
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
        env_file = ".env"
        env_file_encoding = "utf-8"
