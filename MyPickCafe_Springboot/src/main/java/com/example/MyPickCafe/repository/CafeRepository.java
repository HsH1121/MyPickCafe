package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.entity.Cafe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CafeRepository extends JpaRepository<Cafe, Long> {
    List<Cafe> findByStatus(CafeStatus status);

    boolean existsByName(String cafeName);
    boolean existsByNumber(String cafeNumber);


    List<Cafe> findByStatusOrderByViewsDesc(CafeStatus status, Pageable pageable);

    List<Cafe> findByStatusOrderByDateDesc(CafeStatus status, Pageable pageable);

    /** 상태가 같은 카페의 ID 만. 엔티티를 읽지 않으므로 픽봇 호출마다 불러도 가볍다. */
    @Query("select c.id from Cafe c where c.status = :status")
    List<Long> findCafeIdsByStatus(@Param("status") CafeStatus status);

    List<Cafe> findByStatusAndIdInOrderByViewsDesc(CafeStatus status, Collection<Long> cafeIds);

    List<Cafe> findByStatusAndNameContainingOrStatusAndAddressContaining(
            CafeStatus status1, String name, CafeStatus status2, String address);

    long countByStatus(CafeStatus status);

    List<Cafe> findByOwner_IdOrderByDateDesc(Long ownerId);

    // 사업자 증빙 이전(BizDocService.migrateLegacy)용
    List<Cafe> findByBizDocIsNotNull();
}