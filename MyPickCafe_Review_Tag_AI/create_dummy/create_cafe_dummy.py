import random
import time
import ollama

# -----------------------------------------------
# 옵션: 생성할 카페 수 (create_cafeowner_dummy.py 의 CAFEOWNER_COUNT 와 반드시 동일하게 설정)
CAFEOWNER_COUNT = 100
# -----------------------------------------------

CAFE_STYLES = [
    "모던하고 미니멀한 감성 카페 이름 (한글 또는 영문, 예: BLANK, 여백)",
    "따뜻하고 아늑한 분위기 카페 이름 (예: 온기, 포근한오후)",
    "레트로·빈티지 감성 카페 이름 (예: 필름현상소, 67년산커피)",
    "자연·식물 테마 카페 이름 (예: 초록의시간, 이끼정원)",
    "젊고 트렌디한 카페 이름 (예: BEANS CO., 플레이그라운드)",
    "서울 골목 로컬 감성 카페 이름 (예: 연남상회, 익선다실)",
]

SEOUL_LOCATIONS = [
    ("서울시 마포구 연남동",     37.5650, 126.9249),
    ("서울시 마포구 홍대입구",   37.5573, 126.9238),
    ("서울시 강남구 신사동",     37.5220, 127.0209),
    ("서울시 종로구 익선동",     37.5735, 126.9898),
    ("서울시 성동구 성수동",     37.5445, 127.0557),
    ("서울시 용산구 이태원동",   37.5347, 126.9946),
    ("서울시 서대문구 연희동",   37.5685, 126.9395),
    ("서울시 송파구 잠실동",     37.5132, 127.1001),
    ("서울시 광진구 건대입구",   37.5404, 127.0701),
    ("서울시 은평구 불광동",     37.6098, 126.9268),
    ("서울시 영등포구 여의도동", 37.5215, 126.9237),
    ("서울시 강동구 천호동",     37.5384, 127.1238),
]


def generate_cafe_name(style: str, idx: int) -> str:
    try:
        response = ollama.chat(
            model='qwen2.5:7b',
            messages=[
                {
                    'role': 'system',
                    'content': (
                        f"당신은 한국 카페 이름 생성기입니다.\n"
                        f"스타일: {style}\n"
                        f"규칙: 9자 이내, 실제 유명 카페 이름 그대로 사용 금지, 카페 이름만 출력."
                    ),
                },
                {'role': 'user', 'content': "카페 이름 하나 만들어줘."},
            ],
            options={'temperature': 0.9, 'top_p': 0.95, 'num_predict': 20, 'seed': random.randint(1, 999999)},
        )
        raw = response['message']['content'].strip().strip('"\'').split('\n')[0]
        return raw[:9] if raw else f"카페{idx}"
    except Exception as e:
        print(f"  [경고] 카페명 생성 실패: {e}")
        return f"카페{idx}"


sql_lines  = ["-- Cafe Dummy Data", ""]
cafe_names = []
used_names: set = set()

print(f"🚀 카페 더미 데이터 생성 시작 ({CAFEOWNER_COUNT}개)")

for i in range(1, CAFEOWNER_COUNT + 1):
    t     = time.time()
    style = random.choice(CAFE_STYLES)
    name  = generate_cafe_name(style, i)

    while name in used_names:
        name = f"{name[:9]}{random.randint(1, 9)}"
    used_names.add(name)
    cafe_names.append(name)

    base_addr, base_lat, base_lon = random.choice(SEOUL_LOCATIONS)
    lat     = round(base_lat + random.uniform(-0.005, 0.005), 6)
    lon     = round(base_lon + random.uniform(-0.005, 0.005), 6)
    address = f"{base_addr} {random.randint(1, 200)}-{random.randint(1, 50)}"
    phone   = f"02-{random.randint(1000, 9999)}-{random.randint(1000, 9999)}"
    owner_email = f"owner{i:03d}@test.com"

    name_esc = name.replace("'", "''")
    addr_esc = address.replace("'", "''")
    sql_lines.append(
        f"INSERT INTO cafe (owner_id, name, address, lat, lon, number, date, views, code, status) "
        f"VALUES ("
        f"(SELECT member_id FROM member WHERE email = '{owner_email}'), "
        f"'{name_esc}', '{addr_esc}', {lat}, {lon}, '{phone}', SYSTIMESTAMP, 0, '02', 'APPROVED'"
        f");"
    )
    print(f"  [{i:03d}/{CAFEOWNER_COUNT}] {name} | {address} ({time.time()-t:.2f}s)")
    time.sleep(0.1)

with open("cafe_dummy.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(sql_lines))

# review_dummy.py 에서 카페명 참조용
with open("_cafe_names.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(cafe_names))

print(f"\n🎉 cafe_dummy.sql 저장 완료! (총 {CAFEOWNER_COUNT}개)")
