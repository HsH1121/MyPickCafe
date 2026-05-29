package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.CafeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CafeTagRepository extends JpaRepository<CafeTag, Long> {
}
