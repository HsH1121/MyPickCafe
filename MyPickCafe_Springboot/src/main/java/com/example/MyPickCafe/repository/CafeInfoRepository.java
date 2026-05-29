package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.CafeInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CafeInfoRepository extends JpaRepository<CafeInfo, Long> {
    Optional<CafeInfo> findByCafe_Id(Long cafeId);
}