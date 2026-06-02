package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String memberEmail);
    Optional<Member> findByNickname(String candidate);
    boolean existsByNickname(String memberNickname);
    List<Member> findByRoleKind(RoleKind roleKind);

}