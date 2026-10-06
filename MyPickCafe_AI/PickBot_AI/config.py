from pathlib import Path

from pydantic import field_validator
from pydantic_settings import BaseSettings

# MyPickCafe_AI/ — 실행 위치와 무관하게 파일 경로를 풀 기준 디렉터리
_AI_ROOT = Path(__file__).resolve().parent.parent

# .env 는 실행 위치와 무관하게 MyPickCafe_AI/.env 하나만 바라본다.
# (상대경로로 두면 CWD 기준이라 PickBot_AI/ 에서 띄우느냐 루트에서 띄우느냐에 따라
#  다른 파일을 읽고, 설정이 조용히 기본값으로 되돌아간다.)
_ENV_FILE = _AI_ROOT / ".env"


class Settings(BaseSettings):
    # LLM (OpenAI 호환 /chat/completions) — base_url 은 /chat/completions 바로 앞까지
    # 로컬·배포 모두 Fireworks 의 glm-5p3-flash 를 쓴다(2026-09 실측 비교로 선정). 키는 .env 의 LLM_API_KEY.
    # 로컬 Ollama qwen2.5:14b 는 더미 생성(Create_Dummy/local_llm.py)에서만 쓴다.
    llm_base_url:   str = "https://api.fireworks.ai/inference/v1"
    llm_api_key:    str = ""
    llm_model:      str = "accounts/fireworks/models/glm-5p3-flash"
    llm_timeout:    int = 60
    # LLM 1차 호출(질문 분해)의 타임아웃(초). 공용 llm_timeout(60초)을 쓰면 Fireworks 가 가끔 응답을
    # 늦게 줄 때 사용자가 그대로 기다린다(실측: 평소 1~2초인 질문이 한 번 35.1초).
    # 1차 호출은 지역 표현만 뽑고 변환은 지역 맵이 해서 추론이 짧다(region_map.py 참고).
    # 타임아웃이 나면 call_llm 이 한 번 더 시도하고, 그래도 실패하면 질문 전체를 조건으로 보고
    # 지역 필터 없이 진행한다.
    llm_parse_timeout: int = 5
    # 지역 맵에 없는 표현의 동·구를 LLM 에 물을 때의 타임아웃(초). 1차 호출보다 길게 둔다. 맵이 커질수록 드물게 불린다.
    llm_region_timeout: int = 10
    # 지역 확인 호출의 추론량. 지정하지 않으면(모델 기본값) 추론이 129~1000토큰으로 들쭉날쭉해 24번 중 1번은
    # 출력 한도 1000 을 추론에 다 써 빈 응답(JSON 실패)이 났고 최대 9.3초가 걸렸다.
    # medium: 18번 중앙 1.0초·최대 2.7초, 출력 최대 324토큰, 정확도는 기본값과 같았다(low 는 중앙 0.7초).
    llm_region_reasoning_effort: str = "medium"
    # LLM 1차 호출(질문 분해)과 판정 호출(추천 선택)의 추론량. 추론 모델(glm-5p3-flash)은 "low" 가 필요하다
    # (pickbot_rag._PICK_MAX_TOKENS 주석 참고). 비추론 모델(예: Ollama qwen2.5)로 바꾸면 이 값을 보낼 때
    # 400 이 나므로 .env 에 빈 값으로 둔다.
    llm_reasoning_effort: str = "low"

    # 임베딩 (Ollama 네이티브 /api/embed) — LLM 과 다른 서버를 가리킬 수 있다
    # 호스트를 localhost 가 아닌 127.0.0.1 로 둔다. Windows 에서 localhost 는 IPv6(::1) 부터
    # 시도하는데 Ollama 는 IPv4 에서만 대기해, 새 연결마다 약 2.2s 가 붙는다.
    embed_base_url: str = "http://127.0.0.1:11434"
    embed_model:    str = "bge-m3"
    # 소량 임베딩(검색 쿼리·리뷰 1건 upsert)을 CPU 로 돌릴지. 로컬에서 LLM 과 GPU 를
    # 나눠 쓸 때 요청마다 모델을 내렸다 올리는 비용(수 초)을 없앤다.
    # LLM 이 원격이거나 VRAM 이 넉넉한 환경이면 false 로 둬도 된다.
    embed_small_batches_on_cpu: bool = True

    # PostgreSQL (pickbot RAG 인덱싱용) — .env 파일에서 주입
    db_host:     str = "localhost"
    db_port:     int = 5432
    db_name:     str = "mypickcafe"
    db_user:     str = "mypickcafe"
    db_password: str = ""

    # ChromaDB 저장 경로 — 상대경로는 실행 위치가 아니라 MyPickCafe_AI/ 기준으로 푼다.
    # 실행 위치 기준이면 PickBot_AI/ 에서 돌린 embed_all.py 와 MyPickCafe_AI/ 에서
    # 띄운 app.py 가 서로 다른 인덱스를 보고, app.py 가 전체 재인덱싱을 한다.
    chroma_path: str = "./chroma_db"

    # 개발 환경 전용 — 추천 요청마다 단계별 걸린 시간·결과를 PickBot_AI/logs/pickbot_trace.log 와 콘솔에 남긴다.
    # 기본은 끔이고 배포 compose 에는 넣지 않는다. 로컬 MyPickCafe_AI/.env 에서 PICKBOT_TRACE=true 로 켠다.
    pickbot_trace: bool = False

    # 조건별 검색 — 1차 LLM 이 나눈 조건마다 따로 리뷰를 찾고, 후보 리뷰 전부를 모든 조건과 비교해 카페별 커버리지로 순위를 매긴다.
    # 조건마다 가져올 리뷰 수와 그때 카페 하나가 차지할 수 있는 최대 리뷰 수. 21개·카페당 3개면 조건마다 최소 7곳이 잡힌다.
    pickbot_reviews_per_condition: int = 21
    pickbot_search_reviews_per_cafe: int = 3
    # 리뷰가 조건을 충족한 후보로 볼 코사인 유사도 기준. 후보 찾기·순위용이고 최종 의미 판정은 판정 LLM 이 한다.
    # 조건마다 유사도 크기가 달라 0.6 이면 "주차 가능"은 상위 21개가 전부 넘고 "인터넷이 빠름"은 하나도 못 넘어
    # (0.505~0.569) 순위가 한 조건으로 쏠렸다. 조건마다 따로 맞출 수 없으니 낮게 두고 의미 판정은 판정 LLM 에 맡긴다.
    pickbot_condition_threshold: float = 0.4
    # 판정 LLM 에 넘길 상위 카페 수와 카페당 대표 리뷰 수(조건별 근거 리뷰, 있는 만큼만).
    pickbot_judge_max_cafes: int = 7
    pickbot_judge_reviews_per_cafe: int = 3

    # 지역 맵에 없어 LLM 에 물어본 지역 표현을 모으는 파일. 검토해서 PickBot_AI/region_aliases.json 에 옮긴다.
    # 상대경로는 chroma_path 와 같이 MyPickCafe_AI/ 기준이다. 컨테이너에서는 로그 볼륨 안이다.
    region_candidates_path: str = "./PickBot_AI/logs/region_alias_candidates.json"

    @field_validator("chroma_path", "region_candidates_path")
    @classmethod
    def _resolve_path(cls, v: str) -> str:
        p = Path(v)
        return str(p if p.is_absolute() else (_AI_ROOT / p).resolve())

    class Config:
        env_file = _ENV_FILE
        env_file_encoding = "utf-8"
        # 공용 .env 를 여러 패키지가 나눠 쓰므로, 이 클래스가 선언하지 않은
        # 키(다른 패키지용 설정)는 무시한다. 없으면 extra_forbidden 으로 죽는다.
        extra = "ignore"
