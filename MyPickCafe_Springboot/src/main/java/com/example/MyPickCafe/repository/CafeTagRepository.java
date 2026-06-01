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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM cafe_tag WHERE cafe_id = :cafeId", nativeQuery = true)
    void deleteAllByCafeId(@Param("cafeId") Long cafeId);

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
    List<Long> findCafeIdsByTag(@Param("tagCategory") String tagCategory,
                                @Param("tagCode") String tagCode);
}
