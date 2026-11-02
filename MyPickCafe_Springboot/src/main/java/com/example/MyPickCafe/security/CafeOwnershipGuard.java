package com.example.MyPickCafe.security;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 카페 소유권 검증.
 *
 * <p>SecurityConfig의 URL 기반 인가는 "CAFEOWNER 역할인가"까지만 판단할 수 있다.
 * "<b>이</b> 카페의 점주인가"는 리소스를 조회해야 알 수 있으므로 별도 검증이 필요하다.
 * (이 검증이 없으면 점주 A가 점주 B의 카페 메뉴·사진을 수정할 수 있다.)
 *
 * <p>동일한 로직이 여러 컨트롤러에 중복되던 것을 한곳으로 모았다.
 */
@Component
@RequiredArgsConstructor
public class CafeOwnershipGuard {

    private final MemberService memberService;

    /** 요청자가 해당 카페의 점주이거나 관리자가 아니면 {@link AccessDeniedException}. */
    public void ensureOwner(Authentication auth, Cafe cafe) {
        if (!isLoggedIn(auth)) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        if (!canManage(auth, cafe)) {
            throw new AccessDeniedException("해당 카페의 점주만 가능합니다.");
        }
    }

    /** 관리자이거나 이 카페의 점주면 true. 비로그인·익명은 false. */
    public boolean canManage(Authentication auth, Cafe cafe) {
        if (!isLoggedIn(auth) || cafe == null) return false;
        if (isAdmin(auth)) return true;
        Member me = memberService.findByEmailOptional(auth.getName()).orElse(null);
        return me != null
                && cafe.getOwner() != null
                && cafe.getOwner().getId().equals(me.getId());
    }

    /**
     * 카페를 볼 수 있는가. 승인(APPROVED)된 카페는 누구나, 승인 전(PENDING)·반려(REJECTED) 카페는
     * 관리자(심사)와 그 카페의 점주(관리)만 볼 수 있다.
     */
    public boolean canView(Authentication auth, Cafe cafe) {
        if (cafe == null) return false;
        return cafe.isApproved() || canManage(auth, cafe);
    }

    private static boolean isLoggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
