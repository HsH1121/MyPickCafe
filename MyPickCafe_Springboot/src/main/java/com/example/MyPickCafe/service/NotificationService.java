package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.NotificationType;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Notification;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyReviewCreated(Review review) {
        Cafe cafe = review.getCafe();
        if (cafe == null || cafe.getOwner() == null) return;

        Notification n = new Notification();
        n.setRecipient(cafe.getOwner());
        n.setCafe(cafe);
        n.setReview(review);
        n.setType(NotificationType.REVIEW);
        n.setMessage("[" + cafe.getName() + "]에 새 리뷰가 등록되었습니다.");
        notificationRepository.save(n);
    }

    @Transactional
    public void notifyCafeStatus(Cafe cafe, CafeStatus status) {
        if (cafe == null || cafe.getOwner() == null) return;

        Notification n = new Notification();
        n.setRecipient(cafe.getOwner());
        n.setCafe(cafe);
        n.setType(status == CafeStatus.APPROVED ? NotificationType.CAFE_APPROVED : NotificationType.CAFE_REJECTED);
        n.setMessage(status == CafeStatus.APPROVED ? "카페가 승인되었습니다." : "카페가 반려되었습니다.");
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return (email == null) ? 0 : notificationRepository.countByRecipient_EmailAndReadIsFalse(email);
    }

    @Transactional(readOnly = true)
    public List<Notification> recent(String email) {
        return (email == null) ? List.of() : notificationRepository.findTop20ByRecipient_EmailOrderByCreatedAtDesc(email);
    }

    @Transactional
    public void markAsRead(Long id, String email) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        if (!n.getRecipient().getEmail().equals(email)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        n.setRead(true);
    }

    @Transactional
    public void markAllAsRead(String email) {
        var list = notificationRepository.findByRecipient_EmailAndReadIsFalseOrderByCreatedAtDesc(email);
        list.forEach(n -> n.setRead(true));
    }

}
