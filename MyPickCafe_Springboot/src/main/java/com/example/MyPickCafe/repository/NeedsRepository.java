package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.UserNeeds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NeedsRepository extends JpaRepository<UserNeeds, Long> {
}