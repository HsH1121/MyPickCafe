# MyPickCafe AI 통합 서버 — 가상환경 세팅

`MyPickCafe_AI/app.py`(통합 FastAPI, :8000)를 띄우기 위한 환경입니다.
`ChatBot_AI`·`Review_Tag_AI`를 각각 따로 띄울 때는 각 폴더의 `SETUP.md`를 보세요.

---

## 소프트웨어 버전

| 항목 | 버전 |
|------|------|
| Python | 3.11.9 (CPython, 64-bit) |
| 가상환경 위치 | `MyPickCafe_AI/.venv` (`.gitignore` 처리됨) |
| 의존성 목록 | `MyPickCafe_AI/requirements.txt` |

> Python 3.12 이상은 권장하지 않습니다. 3.14에서는 `chromadb`·`psycopg` 등
> 네이티브 확장 패키지의 휠이 준비되지 않아 설치가 실패할 수 있습니다.

---

## 1. 가상환경 생성

Windows에 여러 파이썬이 깔려 있으면 `py` 런처로 버전을 명시합니다.

```powershell
py --list                 # 설치된 버전 확인
cd D:\Workspace\MyPickCafe\MyPickCafe_AI
py -3.11 -m venv .venv
```

---

## 2. 활성화

```powershell
# PowerShell
.\.venv\Scripts\Activate.ps1
```

```bash
# Git Bash
source .venv/Scripts/activate
```

활성화 없이 쓰려면 인터프리터를 직접 지정해도 됩니다.

```powershell
.\.venv\Scripts\python.exe app.py
```

---

## 3. 패키지 설치

```powershell
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

`requirements.txt` 하나에 ChatBot_AI·Review_Tag_AI·Create_Dummy 의존성이
모두 들어 있습니다. 통합 서버는 이 환경 하나로 돌아갑니다.

설치되는 주요 패키지:

| 패키지 | 용도 |
|--------|------|
| `fastapi`, `uvicorn[standard]` | 통합 API 서버 |
| `httpx` | LLM·임베딩 HTTP 호출 |
| `pydantic`, `pydantic-settings` | `config.py`의 `Settings` |
| `chromadb` | RAG 벡터 저장소 |
| `psycopg[binary]` | PostgreSQL 리뷰 인덱싱 |
| `ollama` | Ollama 파이썬 클라이언트 |
| `playwright`, `requests` | 더미 데이터 크롤링 (`Create_Dummy`) |

---

## 4. 환경변수

`MyPickCafe_AI/.env` **하나만** 읽습니다. 실행 위치와 무관합니다.

```powershell
copy .env.example .env
```

키 설명은 `.env.example`의 주석을 참고하세요. 파일이 없으면 `config.py`의
기본값(로컬 Ollama)으로 동작합니다.

---

## 5. 실행

```powershell
cd D:\Workspace\MyPickCafe\MyPickCafe_AI
.\.venv\Scripts\python.exe app.py
```

- 주소: `http://0.0.0.0:8000` (reload 켜짐)
- 헬스체크: `GET http://localhost:8000/health`
- 챗봇·태그 분석 API가 **8000 포트 한 곳**에서 모두 제공됩니다.

---

## 6. 확인

```powershell
.\.venv\Scripts\python.exe -c "import app; print(len(app.app.routes))"
```

라우트 개수가 출력되면 의존성과 `.env` 로딩이 모두 정상입니다.

---

## 주의사항

- **Playwright 브라우저는 따로 받아야 합니다.** `Create_Dummy`의 네이버 크롤링을
  쓸 때만 필요합니다.
  ```powershell
  .\.venv\Scripts\python.exe -m playwright install chromium
  ```
- **ChromaDB 경로는 실행 디렉터리 기준 `./chroma_db`입니다.** `MyPickCafe_AI/`에서
  띄우느냐 `ChatBot_AI/`에서 띄우느냐에 따라 다른 위치에 인덱스가 생깁니다.
  통합 서버는 항상 `MyPickCafe_AI/`에서 실행하세요.
- **Ollama가 별도로 떠 있어야 합니다.** 모델도 미리 받아두세요.
  ```powershell
  ollama pull qwen2.5:14b
  ollama pull bge-m3
  ```
- `.venv`와 `.env`는 `.gitignore` 대상입니다. 커밋되지 않습니다.
