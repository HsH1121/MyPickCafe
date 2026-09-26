package com.example.MyPickCafe.repository;


import com.example.MyPickCafe.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"member", "cafe"})
    List<Review> findByCafe_IdOrderByCreatedAtDesc(Long cafeId);

    /**
     * 최근 리뷰를 작성 시각 내림차순으로 정해진 개수만 조회한다.
     *
     * <p>메인·검색 페이지의 "최근 후기" 영역용. 예전에는 {@code findAll()}로 리뷰 전 행을
     * 엔티티로 읽어 자바에서 정렬하고 6건만 남겼다. 2.8만 건 기준 매 요청마다 약 10MB를
     * 전송하고 엔티티 2.8만 개를 만들던 것을 DB의 정렬 + LIMIT 로 넘긴다.
     *
     * <p>템플릿이 {@code review.cafe.name}을 읽으므로 {@code cafe}를 함께 가져온다.
     * {@code created_at}은 {@code nullable = false}라 NULL 정렬 순서는 문제가 되지 않는다.
     */
    @EntityGraph(attributePaths = {"cafe"})
    List<Review> findTop6ByOrderByCreatedAtDesc();

    int countByCafe_IdAndSentiment(Long cafeId, String sentiment);


    @EntityGraph(attributePaths = {"cafe"})
    Page<Review> findByMember_IdOrderByCreatedAtDesc(Long memberId, Pageable pageable);


    @Query("""
        SELECT r.cafe.id,
               SUM(CASE WHEN r.sentiment = 'GOOD' THEN 1 ELSE 0 END),
               COUNT(r)
        FROM Review r
        WHERE r.cafe.id IN :cafeIds
        GROUP BY r.cafe.id
    """)
    List<Object[]> findSentimentCountsForCafeIds(@Param("cafeIds") Collection<Long> cafeIds);
}