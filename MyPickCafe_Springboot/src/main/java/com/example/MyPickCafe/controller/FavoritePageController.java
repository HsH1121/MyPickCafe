// src/main/java/com/example/MyPickCafe/controller/FavoritePageController.java
package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.service.CafePhotoService;
import com.example.MyPickCafe.service.FavoriteService;
import com.example.MyPickCafe.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FavoritePageController {

    private final FavoriteService favoriteService;
    private final MemberService memberService;
    private final CafePhotoService cafePhotoService;

    @GetMapping("/favorites")
    public String favoritesPage(Authentication auth, Model model) {
        Long memberId = memberService.findByEmail(auth.getName()).getId();

        var page = favoriteService.listMyFavorites(memberId, PageRequest.of(0, 50));

        List<Long> cafeIds = page.stream().map(c -> c.getId()).toList();

        Map<Long, String> mainUrlByCafeId = new LinkedHashMap<>();
        if (!cafeIds.isEmpty()) {
            List<CafePhoto> ordered = cafePhotoService.findForCafeIdsOrderByMainThenSort(cafeIds);
            for (CafePhoto p : ordered) {
                Long cid = p.getCafe().getId();
                mainUrlByCafeId.putIfAbsent(cid, p.getUrl());
            }
        }

        List<CafeDto> favorites = page.stream()
                .map(cafe -> new CafeDto(
                        cafe.getId(),
                        cafe.getName(),
                        cafe.getAddress(),
                        mainUrlByCafeId.get(cafe.getId())
                ))
                .toList();

        model.addAttribute("favorites", favorites);
        model.addAttribute("nav_favorites", true);
        return "member/favorites";
    }

    public record CafeDto(Long cafeId, String cafeName, String cafeAddress, String cafePhoto) {}
}
