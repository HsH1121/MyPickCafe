// src/main/java/com/example/MyPickCafe/repository/ReviewTagRepository.java
package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.ReviewTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewTagRepository extends JpaRepository<ReviewTag, Long> {
    public interface TagCount {
        String getCode();
        Long getCnt();
    }
    /* 좋아요 태그 집계 (GOOD 리뷰만) */
    @Query(value = """
    SELECT t.code AS code, COUNT(*) AS cnt
      FROM review_tag t
      JOIN review r ON r.review_id = t.review_id
     WHERE r.cafe_id = :cafeId
       AND t.category_code = 'LIKE'
       AND r.sentiment = 'GOOD'
     GROUP BY t.code
     ORDER BY cnt DESC
""", nativeQuery = true)
    List<Object[]> findLikeTagCountsGood(@Param("cafeId") Long cafeId);

    /* 카페의 모든 리뷰 태그를 카테고리·코드별 집계 (cnt 내림차순) */
    @Query(value = """
        SELECT t.category_code, t.code, COUNT(*) AS cnt
          FROM review_tag t
          JOIN review r ON r.review_id = t.review_id
         WHERE r.cafe_id = :cafeId
           AND t.category_code IN ('FACILITY','MENU','PURPOSE','MOOD')
         GROUP BY t.category_code, t.code
         ORDER BY t.category_code, cnt DESC
    """, nativeQuery = true)
    List<Object[]> findTagCountsByCafe(@Param("cafeId") Long cafeId);

    /* 리뷰에 달린 태그 전부 삭제 (연관관계 없이 review_id 정리) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM review_tag WHERE review_id = :reviewId", nativeQuery = true)
    void deleteByReviewId(@Param("reviewId") Long reviewId);

}
