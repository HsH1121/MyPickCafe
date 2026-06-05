import random

TEST_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lN07"


def generate_one(owner_num: int) -> str:
    """단일 카페 사장 INSERT SQL 반환"""
    age    = random.randint(35, 55)
    gender = random.choice(['M', 'F'])
    return (
        f'INSERT INTO "member" (email, password, nickname, age, gender, role_kind, created_at, token_version) '
        f"VALUES ('owner{owner_num:03d}@test.com', '{TEST_PASSWORD_HASH}', 'owner{owner_num:03d}', "
        f"{age}, '{gender}', 'CAFEOWNER', SYSTIMESTAMP, 0);"
    )
