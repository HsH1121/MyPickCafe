package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.entity.Cafe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CafeRepository extends JpaRepository<Cafe, Long> {
    List<Cafe> findByStatus(CafeStatus status);

    boolean existsByName(String cafeName);
    boolean existsByNumber(String cafeNumber);

    List<Cafe> findTop8ByOrderByViewsDesc();

    // ✅ [추가] 상태(status)를 기준으로 조회수(views)가 높은 순으로 카페를 찾는 메소드
    List<Cafe> findByStatusOrderByViewsDesc(CafeStatus status, Pageable pageable);


    // ✅ [추가] 상태(status)와 검색어(keyword)를 기준으로 카페를 찾는 메소드
    List<Cafe> findByStatusAndNameContainingOrStatusAndAddressContaining(
            CafeStatus status1, String name, CafeStatus status2, String address);

    // 추가
    long countByStatus(CafeStatus status);
    boolean existsByAddress(String address);
}