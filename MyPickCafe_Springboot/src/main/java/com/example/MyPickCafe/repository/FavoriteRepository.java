package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Favorite;
import com.example.MyPickCafe.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {


    boolean existsByMemberAndCafe(Member member, Cafe cafe);

    long countByCafe(Cafe cafe);

    Page<Favorite> findByMember(Member member, Pageable pageable);

    Optional<Favorite> findByMember_IdAndCafe_Id(Long memberId, Long cafeId);
    boolean existsByMember_IdAndCafe_Id(Long memberId, Long cafeId);
    long countByCafe_Id(Long cafeId);
}
