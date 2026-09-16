from pathlib import Path

from pydantic_settings import BaseSettings

# .env 는 실행 위치와 무관하게 MyPickCafe_AI/.env 하나만 바라본다.
# (Create_Dummy 도 이 설정을 그대로 import 해서 쓴다.)
_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    # LLM (OpenAI 호환 /chat/completions) — base_url 은 /chat/completions 바로 앞까지
    # Review_Tag_AI 는 임베딩을 쓰지 않으므로 embed_base_url 은 두지 않는다.
    llm_base_url:   str = "http://localhost:11434/v1"
    llm_api_key:    str = ""
    ollama_model:   str = "qwen2.5:14b"
    ollama_timeout: int = 60

    class Config:
        env_file = _ENV_FILE
        env_file_encoding = "utf-8"
        # 공용 .env 를 여러 패키지가 나눠 쓰므로, 이 클래스가 선언하지 않은
        # 키(다른 패키지용 설정)는 무시한다. 없으면 extra_forbidden 으로 죽는다.
        extra = "ignore"
