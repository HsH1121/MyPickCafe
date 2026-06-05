import glob
import os
import random
import re

NAVER_CSV_DIR = os.path.join(os.path.dirname(__file__), 'naver')

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


def list_cafes() -> list[tuple[str, str]]:
    """(csv_filename, cafe_name) 목록 반환 (정렬 순서 고정)"""
    csv_files = sorted(glob.glob(os.path.join(NAVER_CSV_DIR, '*.csv')))
    return [
        (os.path.basename(f), re.sub(r'^\d+_', '', os.path.basename(f)).replace('_리뷰.csv', ''))
        for f in csv_files
    ]


def generate_one(cafe_name: str, owner_num: int) -> str:
    """단일 카페 INSERT SQL 반환"""
    base_addr, base_lat, base_lon = random.choice(SEOUL_LOCATIONS)
    lat     = round(base_lat + random.uniform(-0.005, 0.005), 6)
    lon     = round(base_lon + random.uniform(-0.005, 0.005), 6)
    address = f"{base_addr} {random.randint(1, 200)}-{random.randint(1, 50)}"
    phone   = f"02-{random.randint(1000, 9999)}-{random.randint(1000, 9999)}"
    name_esc = cafe_name.replace("'", "''")
    addr_esc = address.replace("'", "''")
    return (
        f'INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) '
        f"VALUES ("
        f"(SELECT member_id FROM member WHERE email = 'owner{owner_num:03d}@test.com'), "
        f"'{name_esc}', '{addr_esc}', {lat}, {lon}, '{phone}', SYSTIMESTAMP, 0, '02', 'APPROVED'"
        f");"
    )
