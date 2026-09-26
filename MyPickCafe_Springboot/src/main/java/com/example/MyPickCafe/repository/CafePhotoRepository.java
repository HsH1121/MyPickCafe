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

    // ❌ existsByCafe_IdAndIsMainTrue -> 필드명은 main
    // ✅
    boolean existsByCafe_IdAndMainTrue(Long cafeId);

    long countByCafe_Id(Long cafeId);

    /**
     * 카페별 대표 사진 URL을 {@code (cafe_id, url)} 쌍으로, 카페당 한 행씩 조회한다.
     *
     * <p>예전에는 해당 카페들의 사진을 전부 엔티티로 읽어(`main desc, sortIndex asc` 정렬)
     * 호출부마다 첫 장만 남기고 버렸다. 카드 목록에 필요한 건 대표 사진 한 장뿐이라
     * {@code DISTINCT ON}으로 DB에서 카페당 한 행만 받는다. 호출부의 중복 제거도 필요 없다.
     *
     * <p>정렬 기준은 {@code is_main}이 true인 사진 우선, 그다음 {@code sort_index} 오름차순.
     * {@code is_main}은 nullable이라 {@code is_main DESC}를 쓰면 안 된다 — PostgreSQL의
     * DESC는 NULLS FIRST라서 {@code is_main}이 NULL인 사진이 대표 사진을 앞지른다.
     * 그래서 {@link #findMainPhoto}와 같은 CASE 식으로 비교한다.
     *
     * <p>{@code cafeIds}가 비어 있으면 {@code IN ()}이 되어 SQL 문법 오류가 난다.
     * {@code CafePhotoService.findMainPhotoUrls}가 빈 컬렉션을 걸러 준다.
     */
    @Query(value = """
        SELECT DISTINCT ON (p.cafe_id) p.cafe_id, p.url
          FROM cafe_photo p
         WHERE p.cafe_id IN (:cafeIds)
         ORDER BY p.cafe_id,
                  CASE WHEN p.is_main = true THEN 0 ELSE 1 END,
                  p.sort_index ASC
    """, nativeQuery = true)
    List<Object[]> findMainPhotoUrlsForCafeIds(@Param("cafeIds") Collection<Long> cafeIds);

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