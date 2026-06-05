import sys
sys.stdout.reconfigure(encoding='utf-8')

import os
import time

from src import create_cafe_dummy
from src import create_cafeowner_dummy
from src import create_user_dummy
from src import create_review_dummy

# -----------------------------------------------
# 옵션: 생성 횟수 설정
MEMBER_COUNT      = 200  # 리뷰 작성자 수 (카페 수는 naver CSV 파일 수로 자동 결정)
MIN_REVIEW_LENGTH = 70   # 포함할 리뷰 최소 글자 수
# -----------------------------------------------

CHECKPOINT_DONE   = "review_checkpoint.txt"
CHECKPOINT_MEMBER = "member_partial.sql"
CHECKPOINT_CAFE   = "cafe_partial.sql"
CHECKPOINT_REVIEW = "review_partial.sql"
OUTPUT_FILE       = "all_dummy.sql"

# --- 체크포인트 상태 로드 ---
done_cafes: set[str] = set()
if os.path.exists(CHECKPOINT_DONE):
    with open(CHECKPOINT_DONE, 'r', encoding='utf-8') as f:
        done_cafes = {line.strip() for line in f if line.strip()}

all_cafes  = create_cafe_dummy.list_cafes()   # [(filename, cafe_name), ...]
cafe_count = len(all_cafes)
new_count  = sum(1 for fn, _ in all_cafes if fn not in done_cafes)

# --- 설정 출력 ---
print("=" * 50)
print("  더미 데이터 생성기 설정")
print(f"  · 리뷰 작성자 수    : {MEMBER_COUNT}명")
print(f"  · 리뷰 최소 글자 수 : {MIN_REVIEW_LENGTH}자")
print()
print("  리뷰 집계 중...", end="", flush=True)
_review_total, _cafe_total = create_review_dummy.count_reviews(MIN_REVIEW_LENGTH)
print(f"\r  · {MIN_REVIEW_LENGTH}자 이상 리뷰    : 총 {_review_total:,}건 / {_cafe_total}개 카페")
if done_cafes:
    print(f"  · 체크포인트        : {len(done_cafes)}/{cafe_count} 완료, {new_count}개 남음")
print("=" * 50)
print()

# --- 1. 멤버(리뷰 작성자) SQL — 최초 1회 생성 후 재사용 ---
if os.path.exists(CHECKPOINT_MEMBER):
    print(f"  ▶ 멤버 데이터 로드 ({MEMBER_COUNT}명)\n")
else:
    member_sql = create_user_dummy.generate(MEMBER_COUNT)
    with open(CHECKPOINT_MEMBER, 'w', encoding='utf-8') as f:
        f.write("-- Member Dummy Data (공통 비밀번호: test1234)\n\n")
        f.write("\n".join(member_sql[2:]))  # 헤더 2줄 제외하고 저장

# --- 체크포인트 파일 초기화 (최초 시작 시만) ---
if not os.path.exists(CHECKPOINT_CAFE):
    with open(CHECKPOINT_CAFE, 'w', encoding='utf-8') as f:
        f.write("-- CafeOwner + Cafe Dummy Data\n\n")
if not os.path.exists(CHECKPOINT_REVIEW):
    with open(CHECKPOINT_REVIEW, 'w', encoding='utf-8') as f:
        f.write("-- Review + ReviewTag Dummy Data\n\n")

# --- 2. 카페 + 리뷰 통합 루프 (카페 단위 체크포인트) ---
total_review = 0
if done_cafes and os.path.exists(CHECKPOINT_REVIEW):
    with open(CHECKPOINT_REVIEW, 'r', encoding='utf-8') as f:
        total_review = sum(1 for line in f if line.startswith("INSERT INTO review "))

mode = "이어서" if done_cafes else "시작"
print(f"🚀 카페 + 리뷰 생성 {mode} ({new_count}개 카페 남음 / 최소 {MIN_REVIEW_LENGTH}자)")

for idx, (filename, cafe_name) in enumerate(all_cafes, 1):
    if filename in done_cafes:
        print(f"  [{idx:03d}/{cafe_count}] {cafe_name} 스킵 (완료됨)")
        continue

    owner_num = idx
    _start    = time.time()

    # 카페 사장 + 카페 SQL 생성
    owner_sql = create_cafeowner_dummy.generate_one(owner_num)
    cafe_sql  = create_cafe_dummy.generate_one(cafe_name, owner_num)

    # 리뷰 SQL 생성 (AI 태그 추출)
    csv_path   = os.path.join(create_review_dummy.NAVER_CSV_DIR, filename)
    review_sql = create_review_dummy.generate_for_cafe(
        csv_path, cafe_name, MEMBER_COUNT, MIN_REVIEW_LENGTH
    )
    total_review += sum(1 for line in review_sql if line.startswith("INSERT INTO review "))

    # 체크포인트에 즉시 저장
    with open(CHECKPOINT_CAFE, 'a', encoding='utf-8') as f:
        f.write(owner_sql + "\n" + cafe_sql + "\n\n")
    with open(CHECKPOINT_REVIEW, 'a', encoding='utf-8') as f:
        f.write("\n".join(review_sql) + "\n")
    with open(CHECKPOINT_DONE, 'a', encoding='utf-8') as f:
        f.write(filename + "\n")
    done_cafes.add(filename)

    _elapsed = time.time() - _start
    print(f"  [{idx:03d}/{cafe_count}] {cafe_name} 완료 ({_elapsed:.1f}s)")

print(f"  ✅ 전체 완료: 카페 {cafe_count}개, 리뷰 {total_review:,}건\n")

# --- 3. 최종 SQL 조합 ---
all_sql = [
    "-- 기존 데이터 초기화 (FK 순서 역순)",
    "DELETE FROM review_tag;",
    "DELETE FROM review;",
    "DELETE FROM cafe;",
    "DELETE FROM member;",
    "",
]

for checkpoint_file in [CHECKPOINT_MEMBER, CHECKPOINT_CAFE, CHECKPOINT_REVIEW]:
    with open(checkpoint_file, 'r', encoding='utf-8') as f:
        all_sql += f.read().splitlines()
    all_sql.append("")

all_sql += ["", "COMMIT;"]

with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
    f.write("\n".join(all_sql))

print(f"🎉 전체 더미 데이터 생성 완료 → {OUTPUT_FILE}")
