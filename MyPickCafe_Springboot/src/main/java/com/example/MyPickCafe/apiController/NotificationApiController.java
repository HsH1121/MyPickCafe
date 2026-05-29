package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.Notification;
import com.example.MyPickCafe.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;

    private String email(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof UserDetails ud) return ud.getUsername();
        return auth.getName();
    }

    @GetMapping
    public List<Notification> list(Authentication auth) {
        return notificationService.recent(email(auth));
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication auth) {
        return notificationService.unreadCount(email(auth));
    }

    @PostMapping("/{id}/read")
    public void read(Authentication auth, @PathVariable Long id) {
        notificationService.markAsRead(id, email(auth));
    }

    @PostMapping("/read-all")
    public void readAll(Authentication auth) {
        notificationService.markAllAsRead(email(auth));
    }
}
