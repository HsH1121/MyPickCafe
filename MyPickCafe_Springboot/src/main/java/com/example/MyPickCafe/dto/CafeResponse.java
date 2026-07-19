package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;

import java.time.LocalDateTime;

/**
 * 카페 조회 응답 DTO.
 *
 * <p>Cafe 엔티티를 그대로 직렬화하면 {@code owner}(Member) 전체가 함께 나가
 * 점주의 이메일·나이·성별까지 노출된다. 여기서는 화면에 필요한
 * ownerId/ownerNickname만 남긴다.
 *
 * <p>{@code bizDoc}(사업자 증빙 문서 경로)은 의도적으로 제외했다.
 * 해당 문서는 관리자 전용 {@code GET /admin/cafes/{id}/bizdoc}으로만 접근한다.
 */
public record CafeResponse(
        Long id,
        Long ownerId,
        String ownerNickname,
        String name,
        String address,
        Double lat,
        Double lon,
        String phone,
        LocalDateTime registeredAt,
        Long views,
        String code,
        String status
) {
    public static CafeResponse from(Cafe c) {
        Member owner = c.getOwner();
        return new CafeResponse(
                c.getId(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getNickname(),
                c.getName(),
                c.getAddress(),
                c.getLat(),
                c.getLon(),
                c.getNumber(),
                c.getDate(),
                c.getViews(),
                c.getCode(),
                c.getStatus() == null ? null : c.getStatus().name()
        );
    }
}
