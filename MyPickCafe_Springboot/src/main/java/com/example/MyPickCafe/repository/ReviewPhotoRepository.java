package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.ReviewPhoto;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, Long> {

    // 화면 표시용: 정렬순(오름차순) + id 보조정렬
    List<ReviewPhoto> findByReview_IdOrderBySortIndexAscIdAsc(Long reviewId);

    // 신규 저장 시 시작 정렬값 계산용 (없으면 -1 반환)
    @Query("""
        select coalesce(max(rp.sortIndex), -1)
        from ReviewPhoto rp
        where rp.review.id = :reviewId
    """)
    Integer findMaxSortIndexByReviewId(@Param("reviewId") Long reviewId);

    // 부모 리뷰 삭제 등에서 한 방에 지울 때
    void deleteByReview_Id(Long reviewId);

    List<ReviewPhoto> findByReview_IdOrderBySortIndexAsc(Long reviewId);
}
