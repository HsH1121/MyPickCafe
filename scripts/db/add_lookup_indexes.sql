-- 운영 DB 에 조회용 인덱스를 추가한다 (perf/lookup-indexes).
--
-- 운영은 spring.jpa.hibernate.ddl-auto=validate 라서 엔티티의 @Index 가 자동으로 생기지 않는다.
-- 로컬(dev, ddl-auto=update)은 앱을 다시 띄우면 Hibernate 가 만든다.
-- 인덱스 이름은 엔티티 선언과 같다. 이름이 다르면 dev 와 운영이 따로 놀게 된다.
--
-- CONCURRENTLY: 만드는 동안 테이블 쓰기를 막지 않는다. 대신 트랜잭션 블록 안에서는 못 쓰므로
-- BEGIN 으로 감싸지 말고 psql 기본(autocommit)으로 실행한다.
--   docker compose exec -T postgres psql -U mypickcafe -d mypickcafe -v ON_ERROR_STOP=1 < scripts/db/add_lookup_indexes.sql
--
-- 도중에 실패하면 INVALID 상태 인덱스가 남을 수 있다. 그때는 DROP INDEX CONCURRENTLY 후 다시 실행한다.
--   SELECT indexrelid::regclass FROM pg_index WHERE NOT indisvalid;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_review_cafe_id_created_at   ON review (cafe_id, created_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_review_member_id_created_at ON review (member_id, created_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_favorite_cafe_id            ON favorite (cafe_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_needs_member_id        ON user_needs (member_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cafe_photo_cafe_id_sort_index ON cafe_photo (cafe_id, sort_index);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cafe_status_views           ON cafe (status, views DESC);

-- 기존 최근 리뷰 인덱스(09-26). 운영 DB 에 있는지 확인되지 않았으므로 함께 둔다. 이미 있으면 건너뛴다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_review_created_at           ON review (created_at DESC);

-- 통계 갱신: 새 인덱스를 플래너가 바로 고려하게 한다.
ANALYZE review;
ANALYZE favorite;
ANALYZE user_needs;
ANALYZE cafe_photo;
ANALYZE cafe;
