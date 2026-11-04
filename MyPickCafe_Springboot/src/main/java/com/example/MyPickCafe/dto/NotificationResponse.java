package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Notification;

import java.time.LocalDateTime;

/**
 * 알림 목록 응답 DTO.
 *
 * <p>Notification 엔티티를 그대로 직렬화하면 recipient·cafe·review 연관을 타고
 * 수신자의 tokenVersion, 카페의 사업자 증빙 경로(bizDoc), 리뷰 작성자(타인)의
 * 이메일·나이·성별까지 응답에 실린다. 알림 드롭다운이 쓰는 필드만 남긴다.
 */
public record NotificationResponse(
        Long id,
        String type,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType() == null ? null : n.getType().name(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
