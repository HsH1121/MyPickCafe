import random

TEST_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lN07"


def generate(count: int) -> list:
    sql_lines = ["-- CafeOwner Dummy Data (공통 비밀번호: test1234)", ""]
    print(f"🚀 [1/4] 카페 사장 더미 데이터 생성 시작 ({count}명)")

    for i in range(1, count + 1):
        age    = random.randint(35, 55)
        gender = random.choice(['M', 'F'])
        sql_lines.append(
            f"INSERT INTO member (email, password, nickname, age, gender, role_kind, created_at, token_version) "
            f"VALUES ('owner{i:03d}@test.com', '{TEST_PASSWORD_HASH}', 'owner{i:03d}', {age}, '{gender}', 'CAFEOWNER', SYSTIMESTAMP, 0);"
        )
        if i % 10 == 0 or i == count:
            print(f"  [{i:03d}/{count}] owner{i:03d}")

    print(f"  ✅ member(CAFEOWNER) {count}건 완료\n")
    return sql_lines
