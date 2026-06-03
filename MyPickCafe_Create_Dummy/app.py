import oracledb
import requests

from src import create_cafeowner_dummy
from src import create_cafe_dummy
from src import create_user_dummy
from src import create_review_dummy

# -----------------------------------------------
# 옵션: 생성 횟수 설정
MEMBER_COUNT = 200  # 리뷰 작성자 수 (카페 수는 naver CSV 파일 수로 자동 결정)
# -----------------------------------------------

# Oracle DB 연결 정보 (FastAPI config.py와 동일)
DB_USER     = "HsH"
DB_PASSWORD = "9048"
DB_DSN      = "localhost:1521/XE"

# FastAPI 서버 주소
FASTAPI_URL = "http://localhost:8000"

OUTPUT_FILE = "all_dummy.sql"

all_sql = [
    "-- 기존 데이터 초기화 (FK 순서 역순)",
    "DELETE FROM review_tag;",
    "DELETE FROM review;",
    "DELETE FROM cafe;",
    "DELETE FROM member;",
    "",
]

# 카페 목록을 먼저 읽어 카페 수 확정
cafe_sql, cafe_names = create_cafe_dummy.generate()
cafe_count = len(cafe_names)

# 1. 카페 사장 (카페 수만큼)
all_sql += create_cafeowner_dummy.generate(cafe_count)
all_sql += ["", ""]

# 2. 카페
all_sql += cafe_sql
all_sql += ["", ""]

# 3. 리뷰 작성자
all_sql += create_user_dummy.generate(MEMBER_COUNT)
all_sql += ["", ""]

# 4. 리뷰 (naver CSV 실제 리뷰)
all_sql += create_review_dummy.generate(MEMBER_COUNT)

all_sql += ["", "COMMIT;"]

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(all_sql))

print(f"🎉 전체 더미 데이터 생성 완료 → {OUTPUT_FILE}")


def execute_sql_to_oracle(sql_lines: list[str]) -> None:
    """생성된 SQL을 Oracle DB에 직접 실행합니다."""
    print("\n⏳ Oracle DB에 더미 데이터 삽입 중...")
    statements = "\n".join(sql_lines)

    with oracledb.connect(user=DB_USER, password=DB_PASSWORD, dsn=DB_DSN) as conn:
        with conn.cursor() as cur:
            for stmt in statements.split(";"):
                stmt = stmt.strip()
                if not stmt or stmt.startswith("--") or stmt.upper() == "COMMIT":
                    continue
                cur.execute(stmt)
        conn.commit()

    print("✅ Oracle DB 삽입 완료")


def trigger_reindex() -> None:
    """FastAPI /chatbot/reindex 를 호출해 ChromaDB 임베딩 인덱싱을 실행합니다."""
    print("\n⏳ ChromaDB 임베딩 인덱싱 요청 중...")
    try:
        res = requests.post(f"{FASTAPI_URL}/chatbot/reindex", timeout=600)
        res.raise_for_status()
        indexed = res.json().get("indexed", "?")
        print(f"✅ ChromaDB 인덱싱 완료: {indexed}건")
    except requests.exceptions.ConnectionError:
        print("⚠️  FastAPI 서버에 연결할 수 없습니다. 서버 실행 후 직접 /chatbot/reindex 를 호출하세요.")
    except Exception as e:
        print(f"⚠️  인덱싱 요청 실패: {e}")


execute_sql_to_oracle(all_sql)
trigger_reindex()
