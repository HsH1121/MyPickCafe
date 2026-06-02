import create_cafeowner_dummy
import create_cafe_dummy
import create_user_dummy
import create_review_dummy

OUTPUT_FILE = "all_dummy.sql"

all_sql = []

# 1. 카페 사장
all_sql += create_cafeowner_dummy.generate()
all_sql += ["", ""]

# 2. 카페 (cafe_names 를 review 생성에 전달)
cafe_sql, cafe_names = create_cafe_dummy.generate()
all_sql += cafe_sql
all_sql += ["", ""]

# 3. 리뷰 작성자
all_sql += create_user_dummy.generate()
all_sql += ["", ""]

# 4. 리뷰 + 태그
all_sql += create_review_dummy.generate(cafe_names)

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(all_sql))

print(f"🎉 전체 더미 데이터 생성 완료 → {OUTPUT_FILE}")
