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

    @Query("""
        select r
        from Review r
        join fetch r.member m
        join fetch r.cafe c
        where c.id = :cafeId
        order by r.createdAt desc
    """)
    List<Review> findByCafeIdWithMember(@Param("cafeId") Long cafeId);

    @EntityGraph(attributePaths = {"member", "cafe"})
    List<Review> findByCafe_IdOrderByCreatedAtDesc(Long cafeId);

    @EntityGraph(attributePaths = {"member", "cafe"})
    List<Review> findTop10ByOrderByCreatedAtDesc();

    int countByCafe_IdAndSentiment(Long cafeId, String sentiment);

    List<Review> findByCafe_IdOrderByIdDesc(Long cafeId);

    @EntityGraph(attributePaths = {"cafe"})
    Page<Review> findByMember_IdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    long countByMember_Id(Long memberId);
    long countByMember_IdAndSentiment(Long memberId, String sentiment);

    @Query("""
        SELECT r.cafe.id,
               SUM(CASE WHEN r.sentiment = 'GOOD' THEN 1 ELSE 0 END),
               COUNT(r)
        FROM Review r
        WHERE r.cafe.id IN :cafeIds
        GROUP BY r.cafe.id
    """)
    List<Object[]> findSentimentCountsByCafeIds(@Param("cafeIds") Collection<Long> cafeIds);
}