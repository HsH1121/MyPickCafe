package com.example.MyPickCafe.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 자주 도는 조회의 인덱스가 엔티티 선언대로 스키마에 생기는지 확인한다.
 *
 * <p>PostgreSQL 은 FK 컬럼에 인덱스를 자동으로 만들지 않는다. 그래서 {@code @Index} 선언이
 * 리팩터링 중에 빠지면 아무 오류 없이 전체 스캔으로 돌아간다. 이 테스트가 그걸 잡는다.
 * 컬럼 순서와 정렬 방향(DESC)까지 본다. 복합 인덱스는 순서가 바뀌면 다른 인덱스가 되기 때문이다.
 */
@SpringBootTest
@ActiveProfiles("test")
class LookupIndexTest {

    @Autowired DataSource dataSource;

    @Test
    void reviewIndexes() throws Exception {
        assertIndex("review", "idx_review_created_at", "created_at DESC");
        assertIndex("review", "idx_review_cafe_id_created_at", "cafe_id ASC", "created_at DESC");
        assertIndex("review", "idx_review_member_id_created_at", "member_id ASC", "created_at DESC");
    }

    @Test
    void favoriteCafeIdIndex() throws Exception {
        assertIndex("favorite", "idx_favorite_cafe_id", "cafe_id ASC");
    }

    @Test
    void userNeedsMemberIdIndex() throws Exception {
        assertIndex("user_needs", "idx_user_needs_member_id", "member_id ASC");
    }

    @Test
    void cafePhotoCafeIdSortIndexIndex() throws Exception {
        assertIndex("cafe_photo", "idx_cafe_photo_cafe_id_sort_index", "cafe_id ASC", "sort_index ASC");
    }

    @Test
    void cafeStatusViewsIndex() throws Exception {
        assertIndex("cafe", "idx_cafe_status_views", "status ASC", "views DESC");
    }

    private void assertIndex(String tableName, String indexName, String... expectedColumns) throws Exception {
        Map<String, List<String>> columnsByIndexName = readIndexColumns(tableName);
        assertThat(columnsByIndexName)
                .as("%s 테이블 인덱스", tableName)
                .containsEntry(indexName, List.of(expectedColumns));
    }

    /** 테이블의 인덱스 이름 → "컬럼 ASC|DESC" 목록(인덱스 안 순서대로). 이름은 소문자로 맞춘다. */
    private Map<String, List<String>> readIndexColumns(String tableName) throws Exception {
        Map<String, List<String>> columnsByIndexName = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String storedTableName = metaData.storesUpperCaseIdentifiers()
                    ? tableName.toUpperCase() : tableName;
            try (ResultSet rs = metaData.getIndexInfo(connection.getCatalog(), null, storedTableName, false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    if (indexName == null || columnName == null) continue;
                    String direction = "D".equals(rs.getString("ASC_OR_DESC")) ? "DESC" : "ASC";
                    int position = rs.getInt("ORDINAL_POSITION");
                    List<String> columns = columnsByIndexName.computeIfAbsent(indexName.toLowerCase(), k -> new ArrayList<>());
                    while (columns.size() < position) columns.add(null);
                    columns.set(position - 1, columnName.toLowerCase() + " " + direction);
                }
            }
        }
        return columnsByIndexName;
    }
}
