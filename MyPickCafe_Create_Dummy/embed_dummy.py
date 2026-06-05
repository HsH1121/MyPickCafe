import sys
sys.stdout.reconfigure(encoding='utf-8')

import requests

# -----------------------------------------------
CHATBOT_URL = "http://localhost:8001"
# -----------------------------------------------

print("=" * 50)
print("  ChromaDB 더미 데이터 일괄 임베딩")
print("=" * 50)
print()
print("  실행 전 확인사항:")
print("  1. all_dummy.sql 을 Oracle DB에 실행 완료")
print("  2. 챗봇 서버 실행 중 (uvicorn app:app --port 8001)")
print()

# 챗봇 서버 연결 확인
try:
    res = requests.get(f"{CHATBOT_URL}/health", timeout=5)
    res.raise_for_status()
    data = res.json()
    print(f"  ✅ 챗봇 서버 연결 확인 (현재 인덱스: {data.get('indexed', 0):,}건)")
except Exception:
    print(f"❌ 챗봇 서버({CHATBOT_URL})에 연결할 수 없습니다.")
    print("   MyPickCafe_ChatBot_AI 디렉터리에서 아래 명령어를 실행하세요:")
    print("   uvicorn app:app --port 8001")
    sys.exit(1)

print()
print("🚀 Oracle DB → ChromaDB 임베딩 시작... (시간이 걸릴 수 있습니다)")

try:
    res = requests.post(f"{CHATBOT_URL}/chatbot/reindex", timeout=1800)
    res.raise_for_status()
    indexed = res.json().get("indexed", "?")
    print(f"🎉 임베딩 완료: {indexed:,}건")
except Exception as e:
    print(f"❌ 임베딩 실패: {e}")
    sys.exit(1)
