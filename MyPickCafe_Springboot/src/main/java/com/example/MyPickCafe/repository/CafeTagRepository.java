package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.CafeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CafeTagRepository extends JpaRepository<CafeTag, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM cafe_tag WHERE cafe_id = :cafeId", nativeQuery = true)
    void deleteAllByCafeId(@Param("cafeId") Long cafeId);
}
