package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.CafeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CafeTagRepository extends JpaRepository<CafeTag, Long> {

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM cafe_tag WHERE cafe_id = :cafeId", nativeQuery = true)
    void deleteCafeTagsForCafeId(@Param("cafeId") Long cafeId);

    @Query(value = """
        SELECT cafe_id,
               COALESCE(
                   CASE WHEN facility_tag IS NOT NULL THEN CONCAT('FACILITY:', facility_tag) END,
                   CASE WHEN menu_tag     IS NOT NULL THEN CONCAT('MENU:',     menu_tag)     END,
                   CASE WHEN purpose_tag  IS NOT NULL THEN CONCAT('PURPOSE:',  purpose_tag)  END,
                   CASE WHEN mood_tag     IS NOT NULL THEN CONCAT('MOOD:',     mood_tag)     END
               ) AS tag_str
          FROM cafe_tag
    """, nativeQuery = true)
    List<Object[]> findAllCafeTagStrings();

    @Query(value = """
        SELECT DISTINCT cafe_id FROM cafe_tag
        WHERE (facility_tag = :tagCode AND :tagCategory = 'FACILITY')
           OR (menu_tag     = :tagCode AND :tagCategory = 'MENU')
           OR (purpose_tag  = :tagCode AND :tagCategory = 'PURPOSE')
           OR (mood_tag     = :tagCode AND :tagCategory = 'MOOD')
    """, nativeQuery = true)
    List<Long> findCafeIdsForTag(@Param("tagCategory") String tagCategory,
                                @Param("tagCode") String tagCode);

    /**
     * 실제로 카페에 붙어 있는 태그 종류만 (카테고리, 코드) 쌍으로 중복 없이 조회한다.
     *
     * <p>화면의 태그 칩 목록용. 예전에는 {@code findAll()}로 cafe_tag 전 행을 엔티티로
     * 읽어 자바에서 distinct를 돌렸지만, 필요한 건 "존재하는 태그 종류"뿐이므로
     * DISTINCT를 DB에 맡긴다. 가로형 테이블이라 카테고리별 UNION이 필요하다.
     */
    @Query(value = """
        SELECT DISTINCT 'MOOD'     AS category_code, mood_tag     AS code FROM cafe_tag WHERE mood_tag     IS NOT NULL
        UNION SELECT DISTINCT 'PURPOSE',  purpose_tag  FROM cafe_tag WHERE purpose_tag  IS NOT NULL
        UNION SELECT DISTINCT 'MENU',     menu_tag     FROM cafe_tag WHERE menu_tag     IS NOT NULL
        UNION SELECT DISTINCT 'FACILITY', facility_tag FROM cafe_tag WHERE facility_tag IS NOT NULL
    """, nativeQuery = true)
    List<Object[]> findTagChipsInUse();
}
