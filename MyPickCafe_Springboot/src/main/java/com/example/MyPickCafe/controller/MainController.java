package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.dto.CafeCardForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.entity.CafeTag;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.service.CafePhotoService;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.CafeTagService;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.service.RecommendService;
import com.example.MyPickCafe.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final CafeService cafeService;
    private final CafeTagService cafeTagService;
    private final ReviewService reviewService;
    private final CafePhotoService cafePhotoService;
    private final MemberService memberService;
    private final RecommendService recommendService;

    @GetMapping({"/", "/main"})
    public String home(Model model, Authentication authentication) {
        List<Cafe> cafes = cafeService.findApprovedTopByViews(8);
        Set<Long> topIds = cafes.stream().map(Cafe::getId).collect(Collectors.toSet());
        List<CafePhoto> mainPhotos = cafePhotoService.findForCafeIdsOrderByMainThenSort(topIds);

        Map<Long, String> photoByCafeId = new HashMap<>();
        for (CafePhoto p : mainPhotos) {
            Long cafeId = p.getCafe().getId();
            photoByCafeId.putIfAbsent(cafeId, p.getUrl());
        }

        final String PLACEHOLDER = "/images/placeholder-cafe.jpg";
        List<CafeCardForm> cafeCards = cafes.stream()
                .map(c -> {
                    String url = photoByCafeId.get(c.getId());
                    String safeUrl = (url == null || url.isBlank()) ? PLACEHOLDER : url;
                    return new CafeCardForm(
                            c.getId(), c.getName(), c.getAddress(),
                            c.getNumber(), c.getCode(), c.getViews(),
                            safeUrl
                    );
                })
                .collect(Collectors.toList());

        List<CafeTag> cafeTags = cafeTagService.findAll().stream()
                .filter(Objects::nonNull)
                .limit(24)
                .collect(Collectors.toList());

        List<Review> recentReviews = reviewService.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());

        model.addAttribute("cafeCards", cafeCards);
        model.addAttribute("cafeTags", cafeTags);
        model.addAttribute("recentReviews", recentReviews);

        // 로그인 사용자에게 자카드 기반 추천 카페
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Member me = memberService.findByEmail(authentication.getName());
            if (me != null) {
                List<CafeCardForm> recommended = recommendService.recommendForMember(me.getId(), 6);
                model.addAttribute("recommendedCafes", recommended);
                model.addAttribute("hasRecommendedCafes", !recommended.isEmpty());
            }
        }

        return "page/main";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String q,
                         @RequestParam(value = "tag", required = false) String tag,
                         @RequestParam(value = "category", required = false) String category,
                         Model model) {

        List<Cafe> results = cafeService.searchApproved(q);

        List<CafeTag> tags = cafeTagService.findAll().stream().limit(24).collect(Collectors.toList());
        List<Review> recent = reviewService.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());

        model.addAttribute("trendingCafes", results);
        model.addAttribute("cafeTags", tags);
        model.addAttribute("recentReviews", recent);
        model.addAttribute("query", q);
        model.addAttribute("selectedTag", tag);
        model.addAttribute("selectedCategory", category);

        return "page/main";
    }
}
