# MyPickCafe 통합 FastAPI 서버 (app.py)
#   - 픽봇 추천(PickBot_AI) + 리뷰 태그 분석(Review_Tag_AI) 을 한 프로세스에서 제공합니다.
#   - 빌드 컨텍스트는 MyPickCafe_AI/ 입니다:  docker build -t mypickcafe-ai:dev .
#
# 이미지에 넣지 않는 것 (.dockerignore):
#   .env(설정은 실행할 때 환경변수로 주입), chroma_db(볼륨), .venv, __pycache__, 로그, Create_Dummy
FROM python:3.11-slim

# 로컬 개발 환경(.venv)이 CPython 3.11 이라 같은 계열을 씁니다.
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# 의존성만 먼저 복사해, 코드만 바뀌었을 때 설치 레이어를 그대로 재사용합니다.
# chromadb·psycopg[binary] 는 휠로 설치되므로 컴파일러가 필요 없습니다.
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# ChromaDB 인덱스와 RAG 선택 로그는 컨테이너 밖(볼륨)에 두는 지점입니다.
# chroma_path 의 상대경로는 MyPickCafe_AI 기준으로 풀리므로 이 이미지에서는 /app/chroma_db 입니다.
RUN mkdir -p /app/chroma_db /app/PickBot_AI/logs \
    && useradd --create-home --uid 10001 appuser \
    && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000

# app.py 의 __main__ 블록은 reload 가 켜져 있어 컨테이너에서는 쓰지 않습니다.
# 워커는 1개로 둡니다. ChromaDB 인덱스를 프로세스 안에서 들고 있어 워커를 늘리면
# 기동 시 초기 인덱싱이 워커 수만큼 중복 실행됩니다.
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]

# 기동 시 인덱스가 비어 있으면 초기 인덱싱을 하므로 start-period 를 넉넉히 둡니다.
HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=3)"
