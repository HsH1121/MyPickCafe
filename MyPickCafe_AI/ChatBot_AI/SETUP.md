# MyPickCafe ChatBot AI — 개발 환경 세팅

## 데스크탑 사양

| 항목 | 사양 |
|------|------|
| CPU | AMD Ryzen 5 5600X (6코어 12스레드, 3.7GHz) |
| RAM | 32GB |
| GPU | NVIDIA GeForce RTX 4070 SUPER |
| VRAM | 12GB |
| OS | Windows 11 Pro (10.0.26200) |
| NVIDIA 드라이버 | 596.49 |

---

## 소프트웨어 버전

| 항목 | 버전 |
|------|------|
| Python | 3.11.9 (CPython) |
| Java | 21.0.10 LTS (Spring Boot용) |
| Ollama | 0.24.0 |

---

## AI 모델

| 항목 | 값 |
|------|-----|
| 채팅 모델 | `qwen2.5:14b` |
| 임베딩 모델 | `nomic-embed-text` |
| Ollama API URL | `http://localhost:11434` |
| VRAM 사용량 (qwen2.5:14b 로드 시) | 약 8.7GB (여유 ~3.3GB) |

---

## Python 가상환경 (.venv)

```
위치: D:\MyPickCafe\MyPickCafe_AI\ChatBot_AI\.venv
베이스: C:\Users\eju20\AppData\Local\Programs\Python\Python311\python.exe
```

### 설치 패키지 (requirements.txt)

```
fastapi>=0.104.0
uvicorn[standard]>=0.24.0
httpx>=0.25.0
pydantic>=2.4.0
pydantic-settings>=2.0.0
chromadb>=0.5.0
oracledb>=2.0.0
ollama>=0.4.0       # embed_all.py 실행 시 필요
```

### 패키지 설치 명령

```powershell
.venv\Scripts\pip.exe install -r requirements.txt
```

---

## FastAPI 서버 설정 (config.py 기본값)

| 설정 | 값 |
|------|----|
| Ollama API URL | `http://localhost:11434` |
| 채팅 모델 | `qwen2.5:14b` |
| 임베딩 모델 | `nomic-embed-text` |
| 타임아웃 | 60초 |
| ChromaDB 경로 | `./chroma_db` |
| Oracle DB DSN | `localhost:1521/XE` |
| 환경변수 파일 | `.env` (없으면 기본값 사용) |

### 서버 실행 명령

```powershell
.venv\Scripts\uvicorn.exe app:app --reload --port 8001
```

### API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/chatbot/recommend` | 카페 추천 (RAG 기반) |
| POST | `/chatbot/reindex` | ChromaDB 전체 재인덱싱 |
| GET | `/health` | 헬스체크 |

---

## Spring Boot 연동

- Spring Boot → `POST http://localhost:8001/chatbot/recommend` 호출
- Request: `{ "query": string }`
- Response: `{ "results": [{"cafeId": int, "cafeName": string, "address": string, "snippet": string, "score": float}] }`

---

## 임베딩 실행 (embed_all.py)

Oracle DB의 리뷰 전체를 ChromaDB에 임베딩합니다. FastAPI 서버 없이 독립 실행 가능.

```powershell
# 신규 리뷰만 추가
.venv\Scripts\python.exe embed_all.py

# ChromaDB 초기화 후 전체 재임베딩
.venv\Scripts\python.exe embed_all.py --reset
```

---

## 실행 전 체크리스트

- [ ] Oracle DB 실행 중 확인 (리뷰 데이터 INSERT 완료)
- [ ] Ollama 서비스 실행 중 확인 (`ollama list` 로 확인)
- [ ] `qwen2.5:14b` 모델 로드 확인
- [ ] `nomic-embed-text` 모델 다운로드 확인 (`ollama pull nomic-embed-text`)
- [ ] `embed_all.py` 실행하여 ChromaDB 인덱싱 완료
- [ ] Spring Boot 서버 실행 (포트 확인 필요)
- [ ] FastAPI 서버 실행 (`uvicorn app:app --reload --port 8001`)
