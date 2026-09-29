package com.example.MyPickCafe.config;

import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    @ModelAttribute
    public void injectAuthInfo(Model model, Authentication authentication) {
        boolean isLoggedIn = authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();

        String email = null;
        String nickname = "게스트";
        if (isLoggedIn) {
            Object principal = authentication.getPrincipal();
            String username = (principal instanceof UserDetails ud) ? ud.getUsername() : authentication.getName();
            email = username;
            // 헤더 인사말은 회원 닉네임 기준. 조회 실패 시에만 이메일 앞부분으로 대체
            nickname = memberRepository.findByEmail(username)
                    .map(Member::getNickname)
                    .orElseGet(() -> {
                        int at = username.indexOf('@');
                        return at > 0 ? username.substring(0, at) : username;
                    });
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
