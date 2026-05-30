package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.dto.ReviewForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final MemberService memberService;
    private final CafeService cafeService;

    @GetMapping("/new")
    public String newForm(@RequestParam("cafeId") Long cafeId) {
        return "redirect:/cafes/" + cafeId + "#reviewModal";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute ReviewForm form,
                         @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                         RedirectAttributes ra,
                         Authentication authentication,
                         HttpServletRequest request) {

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/login?error=" + URLEncoder.encode("로그인이 필요한 서비스입니다.", StandardCharsets.UTF_8)
                    + "&redirect=" + URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8);
        }

        if (form.getCafeId() == null) {
            String fromMain = request.getParameter("cafeId");
            if (fromMain != null && !fromMain.isBlank()) {
                form.setCafeId(Long.valueOf(fromMain));
            } else {
                String ref = request.getHeader("Referer");
                if (ref != null) {
                    Matcher m = Pattern.compile("/cafes/(\\d+)").matcher(ref);
                    if (m.find()) form.setCafeId(Long.valueOf(m.group(1)));
                }
            }
            if (form.getCafeId() == null) {
                ra.addFlashAttribute("message", "카페 정보가 유실되어 리뷰를 저장하지 못했습니다. 다시 시도해주세요.");
                return "redirect:/";
            }
        }

        Member me = memberService.findByEmail(authentication.getName());
        Cafe cafe = cafeService.findById(form.getCafeId());

        String s = (form.getSentiment() != null) ? form.getSentiment() : request.getParameter("sentiment");
        if (s != null) s = s.trim().toUpperCase();
        if (!"GOOD".equals(s) && !"BAD".equals(s)) s = null;

        reviewService.saveWithTags(form, me, cafe, s);

        ra.addFlashAttribute("message", "리뷰가 등록되었습니다.");
        return "redirect:/cafes/" + form.getCafeId();
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute ReviewForm form,
                       RedirectAttributes ra,
                       Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String s = form.getSentiment();
        if (s != null) s = s.trim().toUpperCase();
        if (!"GOOD".equals(s) && !"BAD".equals(s)) s = null;

        reviewService.updateWithTags(id, form, s);

        ra.addFlashAttribute("message", "리뷰가 수정되었습니다.");
        return "redirect:/cafes/" + form.getCafeId();
    }

    @GetMapping("/cafes/{cafeId}/reviews")
    public String list(@PathVariable Long cafeId, Model model) {
        List<Review> reviews = reviewService.findByCafeIdWithMember(cafeId);
        model.addAttribute("reviews", reviews);
        return "reviews/list";
    }
}
