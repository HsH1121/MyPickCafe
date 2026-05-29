package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.service.FavoriteService;
import com.example.MyPickCafe.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final MemberService memberService;

    private Long currentMemberId(Authentication authentication) {
        return memberService.findByEmail(authentication.getName()).getId();
    }

    @PostMapping("/{cafeId}/favorite")
    public Map<String, Object> toggle(@PathVariable Long cafeId, Authentication auth) {
        Long memberId = memberService.findByEmail(auth.getName()).getId();
        boolean favorited = favoriteService.toggle(memberId, cafeId);
        long count = favoriteService.countByCafe(cafeId);
        return Map.of("favorited", favorited, "favoriteCount", count);
    }

    @GetMapping
    public Page<Cafe> myFavorites(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "12") int size,
                                  Authentication auth) {
        return favoriteService.listMyFavorites(currentMemberId(auth), PageRequest.of(page, size));
    }

    @GetMapping("/cafes/{cafeId}/count")
    public Map<String, Long> countFavorited(@PathVariable Long cafeId) {
        return Map.of("count", favoriteService.countFavoriteForCafe(cafeId));
    }
}
