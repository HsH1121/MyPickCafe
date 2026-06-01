package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.entity.Cafe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CafeRepository extends JpaRepository<Cafe, Long> {
    List<Cafe> findByStatus(CafeStatus status);

    boolean existsByName(String cafeName);
    boolean existsByNumber(String cafeNumber);

    List<Cafe> findTop8ByOrderByViewsDesc();

    List<Cafe> findByStatusOrderByViewsDesc(CafeStatus status, Pageable pageable);

    List<Cafe> findByStatusOrderByDateDesc(CafeStatus status, Pageable pageable);

    List<Cafe> findByStatusAndIdInOrderByViewsDesc(CafeStatus status, Collection<Long> ids);

    List<Cafe> findByStatusAndNameContainingOrStatusAndAddressContaining(
            CafeStatus status1, String name, CafeStatus status2, String address);

    long countByStatus(CafeStatus status);
    boolean existsByAddress(String address);
}