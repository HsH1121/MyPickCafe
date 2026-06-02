import create_cafeowner_dummy
import create_cafe_dummy
import create_user_dummy
import create_review_dummy

# -----------------------------------------------
# 옵션: 생성 횟수 설정
CAFEOWNER_COUNT = 50    # 카페 사장 수 (= 카페 수)
MEMBER_COUNT    = 500  # 리뷰 작성자 수
TOTAL_REVIEWS   = 1000    # 생성할 리뷰 수
# -----------------------------------------------

OUTPUT_FILE = "all_dummy.sql"

all_sql = [
    "-- 기존 데이터 초기화 (FK 순서 역순)",
    "DELETE FROM review_tag;",
    "DELETE FROM review;",
    "DELETE FROM cafe;",
    "DELETE FROM member;",
    "",
]

# 1. 카페 사장
all_sql += create_cafeowner_dummy.generate(CAFEOWNER_COUNT)
all_sql += ["", ""]

# 2. 카페 (cafe_names 를 review 생성에 전달)
cafe_sql, cafe_names = create_cafe_dummy.generate(CAFEOWNER_COUNT)
all_sql += cafe_sql
all_sql += ["", ""]

# 3. 리뷰 작성자
all_sql += create_user_dummy.generate(MEMBER_COUNT)
all_sql += ["", ""]

# 4. 리뷰 + 태그
all_sql += create_review_dummy.generate(cafe_names, TOTAL_REVIEWS, MEMBER_COUNT)

all_sql += ["", "COMMIT;"]

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(all_sql))

print(f"🎉 전체 더미 데이터 생성 완료 → {OUTPUT_FILE}")
