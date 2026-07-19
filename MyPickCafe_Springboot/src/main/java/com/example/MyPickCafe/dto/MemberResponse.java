package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Member;

import java.time.LocalDateTime;

/**
 * 회원 조회 응답 DTO.
 *
 * <p>Member 엔티티를 그대로 직렬화하면 {@code password}(BCrypt 해시)와
 * {@code tokenVersion} 같은 내부 필드까지 노출되므로, 외부에 공개해도 되는
 * 필드만 이 DTO로 추려서 응답한다.
 */
public record MemberResponse(
        Long id,
        String email,
        String nickname,
        Long age,
        String gender,
        String roleKind,
        LocalDateTime createdAt,
        String photo
) {
    public static MemberResponse from(Member m) {
        return new MemberResponse(
                m.getId(),
                m.getEmail(),
                m.getNickname(),
                m.getAge(),
                m.getGender(),
                m.getRoleKind() == null ? null : m.getRoleKind().name(),
                m.getCreatedAt(),
                m.getPhoto()
        );
    }
}
