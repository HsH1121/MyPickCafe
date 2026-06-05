import random

TEST_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lN07"


def generate(count: int) -> list:
    sql_lines = ["-- Member Dummy Data (공통 비밀번호: test1234)", ""]
    print(f"🚀 [3/4] 리뷰 작성자 더미 데이터 생성 시작 ({count}명)")

    for i in range(1, count + 1):
        age    = random.randint(15, 49)
        gender = random.choice(['M', 'F'])
        sql_lines.append(
            f"INSERT INTO member (email, password, nickname, age, gender, role_kind, created_at, token_version) "
            f"VALUES ('user{i:05d}@test.com', '{TEST_PASSWORD_HASH}', 'user{i:05d}', {age}, '{gender}', 'MEMBER', SYSTIMESTAMP, 0);"
        )

    print(f"  ✅ member(MEMBER) {count}건 완료\n")
    return sql_lines
