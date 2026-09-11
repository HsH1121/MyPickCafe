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