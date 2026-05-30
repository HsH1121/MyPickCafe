package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.UserNeeds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NeedsRepository extends JpaRepository<UserNeeds, Long> {
    List<UserNeeds> findByMember_Id(Long memberId);

    @Transactional
    void deleteByMember_Id(Long memberId);
}