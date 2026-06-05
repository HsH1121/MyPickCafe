# MyPickCafe Review Tag AI — 개발 환경 세팅

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
| 모델명 | `qwen2.5:14b` |
| 모델 크기 | 8.7GB |
| Ollama API URL | `http://localhost:11434` |
| VRAM 사용량 (로드 시) | 약 8.7GB (여유 ~3.3GB) |

---

## Python 가상환경 (.venv)

```
위치: D:\MyPickCafe\MyPickCafe_Review_Tag_AI\.venv
베이스: C:\Users\eju20\AppData\Local\Programs\Python\Python311\python.exe
```

### 설치 패키지 (requirements.txt)

```
fastapi>=0.104.0
uvicorn[standard]>=0.24.0
httpx>=0.25.0
pydantic>=2.4.0
pydantic-settings>=2.0.0
ollama          # create_review_dummy.py 실행 시 필요
```

### 패키지 설치 명령

```powershell
.venv\Scripts\pip.exe install -r requirements.txt
.venv\Scripts\pip.exe install ollama   # ollama 패키지 별도 설치 (requirements.txt에 미포함)
```

---

## FastAPI 서버 설정 (config.py 기본값)

| 설정 | 값 |
|------|----|
| Ollama API URL | `http://localhost:11434` |
| 모델명 | `qwen2.5:7b` |
| 타임아웃 | 60초 |
| 환경변수 파일 | `.env` (없으면 기본값 사용) |

### 서버 실행 명령

```powershell
.venv\Scripts\uvicorn.exe app:app --reload --port 8000
```

### API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/review/analyze` | 리뷰 분석 (태그 + 감성 추출) |
| GET | `/health` | 헬스체크 |

---

## Spring Boot 연동

- Spring Boot → `POST http://localhost:8000/review/analyze` 호출
- Request: `{ "reviewId": int, "reviewText": string }`
- Response: `{ "sentiment": "GOOD"/"BAD"/null, "facilityTags": [], "menuTags": [], "purposeTags": [], "moodTags": [] }`

---

## 리뷰 더미 데이터 생성 (create_review_dummy.py)

- `ollama` Python 패키지 직접 사용 (httpx 아님)
- 모델: `qwen2.5:7b`
- 생성 수량: 20개 (TOTAL_REVIEWS_TO_GENERATE 변수로 조정)
- 출력 파일: `reviews_dummy_data.json`

```powershell
.venv\Scripts\python.exe create_review_dummy.py
```

---

## 실행 전 체크리스트

- [ ] Ollama 서비스 실행 중 확인 (`ollama list` 로 확인)
- [ ] `qwen2.5:7b` 모델 로드 확인
- [ ] `.venv`에 `ollama` 패키지 설치 확인
- [ ] Spring Boot 서버 실행 (포트 확인 필요)
- [ ] FastAPI 서버 실행 (`uvicorn app:app --reload --port 8000`)
