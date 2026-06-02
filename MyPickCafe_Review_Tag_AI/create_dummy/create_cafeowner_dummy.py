import random

# -----------------------------------------------
# 옵션: 생성할 카페 사장 수
CAFEOWNER_COUNT = 100
# -----------------------------------------------

TEST_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lN07"

sql_lines = ["-- CafeOwner Dummy Data (공통 비밀번호: test1234)", ""]

print(f"🚀 카페 사장 더미 데이터 생성 시작 ({CAFEOWNER_COUNT}명)")

for i in range(1, CAFEOWNER_COUNT + 1):
    age    = random.randint(35, 55)
    gender = random.choice(['M', 'F'])
    sql_lines.append(
        f"INSERT INTO member (email, password, nickname, age, gender, role_kind, created_at, token_version) "
        f"VALUES ('owner{i:03d}@test.com', '{TEST_PASSWORD_HASH}', 'owner{i:03d}', {age}, '{gender}', 'CAFEOWNER', SYSTIMESTAMP, 0);"
    )
    print(f"  [{i:02d}/{CAFEOWNER_COUNT}] owner{i:03d}")

with open("cafeowner_dummy.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql_lines))

print(f"\n🎉 cafeowner_dummy.sql 저장 완료! (총 {CAFEOWNER_COUNT}명)")
