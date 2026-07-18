import random

TEST_PASSWORD_HASH = "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW"


def generate(count: int) -> list:
    sql_lines = []

    for i in range(1, count + 1):
        age    = random.randint(15, 49)
        gender = random.choice(['M', 'F'])
        sql_lines.append(
            f'INSERT INTO member (email, password, nickname, age, gender, role_kind, created_at, token_version) '
            f"VALUES ('user{i:05d}@test.com', '{TEST_PASSWORD_HASH}', 'user{i:05d}', {age}, '{gender}', 'MEMBER', CURRENT_TIMESTAMP, 0);"
        )

    return sql_lines
