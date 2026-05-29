package com.example.MyPickCafe.config;

import com.example.MyPickCafe.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalViewAdvice {

    private final NotificationService notificationService;

    @ModelAttribute
    public void injectAuthInfo(Model model, Authentication authentication) {
        boolean isLoggedIn = authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();

        String email = null;
        String nickname = "게스트";
        if (isLoggedIn) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails ud) {
                email = ud.getUsername();
                nickname = ud.getUsername();
                int at = nickname.indexOf('@');
                nickname = at > 0 ? nickname.substring(0, at) : nickname;
            } else {
                email = authentication.getName();
                nickname = email;
            }
        }

        boolean isAdmin = isLoggedIn && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUserEmail", email);
        model.addAttribute("currentUserNickname", nickname);

        long unread = isLoggedIn ? notificationService.unreadCount(email) : 0;
        model.addAttribute("notificationCount", unread); // ✅ 헤더에서 사용
    }
}
