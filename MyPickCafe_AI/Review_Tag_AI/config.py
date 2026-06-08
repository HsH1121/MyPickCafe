from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    model_api_url:  str = "http://localhost:11434"
    ollama_model:   str = "qwen2.5:14b"
    ollama_timeout: int = 60

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
