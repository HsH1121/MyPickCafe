import sys
sys.stdout.reconfigure(encoding='utf-8')

import os
import re
import time

import ollama

from src import create_cafe_dummy
from src import create_cafeowner_dummy
from src import create_user_dummy
from src import create_review_dummy

# -----------------------------------------------
# 옵션: 생성 횟수 설정
MEMBER_COUNT      = 100  # 리뷰 작성자 수 (카페 수는 naver CSV 파일 수로 자동 결정)
MIN_REVIEW_LENGTH = 70   # 포함할 리뷰 최소 글자 수
# -----------------------------------------------

CHECKPOINT_DONE      = "review_checkpoint.txt"
CHECKPOINT_MEMBER    = "reviewer_dummy.sql"
CHECKPOINT_CAFEOWNER = "cafeowner_dummy.sql"
CHECKPOINT_CAFE      = "cafe_dummy.sql"
CHECKPOINT_REVIEW    = "review_dummy.sql"

# --- Ollama 연결 확인 ---
print("  Ollama 연결 확인 중...")
try:
    ollama.list()

except Exception:
    print("❌ Ollama가 실행되지 않았습니다.")
    print("   'ollama serve' 명령어 또는 Ollama 앱을 실행한 뒤 다시 시도하세요.")
    sys.exit(1)


def _owner_num(filename: str) -> int:
    m = re.match(r'^(\d+)_', filename)
    return int(m.group(1)) if m else 0


def _load_done_cafes() -> set[str]:
    """완료된 카페 목록 로드 + 불완전한 마지막 블록 제거"""
    done = set()
    if os.path.exists(CHECKPOINT_DONE):
        with open(CHECKPOINT_DONE, 'r', encoding='utf-8') as f:
            done = {line.strip() for line in f if line.strip()}

    done_count = len(done)

    # review_dummy.sql: DONE 마커 이후 불완전한 내용 제거
    # (DONE 마커가 없는데 내용이 있으면 헤더만 남김)
    if os.path.exists(CHECKPOINT_REVIEW):
        with open(CHECKPOINT_REVIEW, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        last_done_line = -1
        for i, line in enumerate(lines):
            if line.startswith('-- DONE:'):
                last_done_line = i
        if last_done_line < len(lines) - 1:
            with open(CHECKPOINT_REVIEW, 'w', encoding='utf-8') as f:
                if last_done_line >= 0:
                    f.writelines(lines[:last_done_line + 1])
                else:
                    f.writelines(lines[:2])  # 헤더 + 빈 줄만 유지

    # cafeowner/cafe: done_count 초과분 항상 제거 (중간 중단 시 롤백)
    def _trim_file(path, marker_fn):
        if not os.path.exists(path):
            return
        with open(path, 'r', encoding='utf-8') as f:
            file_lines = f.readlines()
        if sum(1 for l in file_lines if marker_fn(l)) > done_count:
            kept, cnt = [], 0
            for line in file_lines:
                if marker_fn(line):
                    cnt += 1
                    if cnt > done_count:
                        break
                kept.append(line)
            with open(path, 'w', encoding='utf-8') as f:
                f.writelines(kept)
            print(f"  [정리] 불완전한 마지막 블록 제거됨")

    _trim_file(CHECKPOINT_CAFEOWNER, lambda l: l.startswith('INSERT INTO member') and 'owner' in l)
    _trim_file(CHECKPOINT_CAFE,      lambda l: l.startswith('INSERT INTO cafe'))

    return done


# --- 완료 카페 로드 ---
all_cafes  = create_cafe_dummy.list_cafes()   # [(filename, cafe_name), ...]
cafe_count = len(all_cafes)
done_cafes = _load_done_cafes()
new_count  = cafe_count - len(done_cafes)

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

# --- 1. 멤버 SQL — 최초 1회 생성 후 재사용 ---
if os.path.exists(CHECKPOINT_MEMBER):
    print(f"  ▶ 멤버 데이터 로드 ({MEMBER_COUNT}명)\n")
else:
    member_sql = create_user_dummy.generate(MEMBER_COUNT)
    with open(CHECKPOINT_MEMBER, 'w', encoding='utf-8') as f:
        f.write("\n".join(member_sql))
    print(f"  ✅ 멤버 {MEMBER_COUNT}명 생성 완료\n")


# --- 카페/리뷰 파일 초기화 (최초 시작 시만) ---
if not os.path.exists(CHECKPOINT_CAFEOWNER):
    with open(CHECKPOINT_CAFEOWNER, 'w', encoding='utf-8') as f:
        f.write("-- CafeOwner Dummy Data\n\n")
if not os.path.exists(CHECKPOINT_CAFE):
    with open(CHECKPOINT_CAFE, 'w', encoding='utf-8') as f:
        f.write("-- Cafe Dummy Data\n\n")
if not os.path.exists(CHECKPOINT_REVIEW):
    with open(CHECKPOINT_REVIEW, 'w', encoding='utf-8') as f:
        f.write("-- Review + ReviewTag Dummy Data\n\n")

# --- 2. 카페 + 리뷰 통합 루프 ---
total_review = 0
if done_cafes and os.path.exists(CHECKPOINT_REVIEW):
    with open(CHECKPOINT_REVIEW, 'r', encoding='utf-8') as f:
        total_review = sum(1 for line in f if line.startswith("INSERT INTO review "))

mode = "이어서" if done_cafes else "시작"
print(f"  카페사장 + 카페 + 리뷰 생성 {mode} ({new_count}개 카페 남음 / 최소 {MIN_REVIEW_LENGTH}자)")

for idx, (filename, cafe_name) in enumerate(all_cafes, 1):
    if filename in done_cafes:
        continue

    owner_num = _owner_num(filename)
    _start    = time.time()

    owner_sql  = create_cafeowner_dummy.generate_one(owner_num)
    cafe_sql   = create_cafe_dummy.generate_one(cafe_name, owner_num)
    csv_path   = os.path.join(create_review_dummy.NAVER_CSV_DIR, filename)
    review_sql = create_review_dummy.generate_for_cafe(
        csv_path, cafe_name, MEMBER_COUNT, MIN_REVIEW_LENGTH
    )
    total_review += sum(1 for line in review_sql if line.startswith("INSERT INTO review "))

    with open(CHECKPOINT_CAFEOWNER, 'a', encoding='utf-8') as f:
        f.write(owner_sql + "\n")
    with open(CHECKPOINT_CAFE, 'a', encoding='utf-8') as f:
        f.write(cafe_sql + "\n\n")
    with open(CHECKPOINT_REVIEW, 'a', encoding='utf-8') as f:
        f.write("\n".join(review_sql) + "\n")
        f.write(f"-- DONE:{filename}\n\n")
    with open(CHECKPOINT_DONE, 'a', encoding='utf-8') as f:
        f.write(filename + "\n")
    done_cafes.add(filename)
    _elapsed = time.time() - _start
    print(f"  [{idx:03d}/{cafe_count}] ✅ {cafe_name} ({_elapsed:.1f}s)")
