import random

# -----------------------------------------------
# 옵션: 생성할 리뷰 작성자 수
MEMBER_COUNT = 10000
# -----------------------------------------------

TEST_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lN07"

sql_lines = ["-- Member Dummy Data (공통 비밀번호: test1234)", ""]

print(f"🚀 리뷰 작성자 더미 데이터 생성 시작 ({MEMBER_COUNT}명)")

for i in range(1, MEMBER_COUNT + 1):
    age    = random.randint(15, 49)
    gender = random.choice(['M', 'F'])
    sql_lines.append(
        f"INSERT INTO member (email, password, nickname, age, gender, role_kind, created_at, token_version) "
        f"VALUES ('user{i:05d}@test.com', '{TEST_PASSWORD_HASH}', 'user{i:05d}', {age}, '{gender}', 'MEMBER', SYSTIMESTAMP, 0);"
    )
    print(f"  [{i:05d}/{MEMBER_COUNT}] user{i:05d}")

with open("user_dummy.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql_lines))

print(f"\n🎉 user_dummy.sql 저장 완료! (총 {MEMBER_COUNT}명)")
