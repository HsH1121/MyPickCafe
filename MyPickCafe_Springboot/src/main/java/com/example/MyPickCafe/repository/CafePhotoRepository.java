package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.CafePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CafePhotoRepository extends JpaRepository<CafePhoto, Long> {

    List<CafePhoto> findByCafe_Id(Long cafeId);
    List<CafePhoto> findByCafe_IdOrderBySortIndexAsc(Long cafeId);

    // ❌ findFirstByCafe_IdAndIsMainTrueOrderBySortIndexAsc -> 필드명은 main
    // ✅ 필요하면 이렇게 쓰세요
    Optional<CafePhoto> findFirstByCafe_IdAndMainTrueOrderBySortIndexAsc(Long cafeId);

    Optional<CafePhoto> findFirstByCafe_IdOrderBySortIndexAsc(Long cafeId);

    // ❌ existsByCafe_IdAndIsMainTrue -> 필드명은 main
    // ✅
    boolean existsByCafe_IdAndMainTrue(Long cafeId);

    long countByCafe_Id(Long cafeId);

    // (서브쿼리에 alias 필수: MySQL 등)
    @Query(value = """
        SELECT * FROM (
          SELECT cp.*,
                 ROW_NUMBER() OVER (
                   PARTITION BY cp.cafe_id
                   ORDER BY CASE WHEN cp.is_main = true THEN 0 ELSE 1 END,
                            cp.sort_index ASC,
                            cp.cafe_photo_id ASC
                 ) AS rn
          FROM cafe_photo cp
        ) t
        WHERE rn = 1
        """, nativeQuery = true)
    List<CafePhoto> findMainPhotosForAllCafes();

    // ❌ p.isMain -> 엔티티 필드명은 main, JPQL은 엔티티 필드명을 사용
    // ✅
    @Query("select p from CafePhoto p " +
            "where p.cafe.id in :cafeIds " +
            "order by p.cafe.id asc, p.main desc, p.sortIndex asc")
    List<CafePhoto> findForCafeIdsOrderByMainThenSort(@Param("cafeIds") Collection<Long> cafeIds);

    @Query(value = """
        SELECT p.*
          FROM cafe_photo p
         WHERE p.cafe_id = :cafeId
         ORDER BY CASE WHEN p.is_main = true THEN 0 ELSE 1 END,
                  p.sort_index ASC
         FETCH FIRST 1 ROWS ONLY
    """, nativeQuery = true)
    CafePhoto findMainPhoto(@Param("cafeId") Long cafeId);

    Optional<CafePhoto> findFirstByCafe_IdOrderBySortIndexDesc(Long cafeId);

    // 이미 올바른 메서드 (필드명 main 사용)
    Optional<CafePhoto> findByCafe_IdAndMainTrue(Long cafeId);
}
