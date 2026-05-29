// src/main/java/com/example/MyPickCafe/controller/CafeController.java
package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.*;
import com.example.MyPickCafe.support.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cafes")
public class CafeController {

    private final CafeService cafeService;
    private final MemberService memberService;
    private final ReviewService reviewService;
    private final CafeStatsService cafeStatsService;
    private final ReviewPhotoService reviewPhotoService;
    private final FavoriteService favoriteService;
    private final CafePhotoService cafePhotoService;
    private final CafeInfoService cafeInfoService;
    private final MenuService menuService;

    /** 신규 등록 폼 */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/new")
    public String createCafeForm() {
        return "cafes/create";
    }

    /** 카페 등록 */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createCafe(Authentication auth,
                             @Valid @ModelAttribute("form") CafeForm form,
                             @RequestParam(value = "cafePhotoFile", required = false) MultipartFile cafePhotoFile,
                             @RequestParam(value = "bizDocFile", required = false) MultipartFile bizDocFile,
                             RedirectAttributes ra) {

        if (auth == null || !auth.isAuthenticated()) {
            ra.addFlashAttribute("msg", "로그인이 필요한 서비스입니다.");
            return "redirect:/login";
        }

        String email = auth.getName();
        Member me = memberService.findByEmail(email);
        if (me == null) {
            ra.addFlashAttribute("msg", "로그인 정보를 찾을 수 없습니다.");
            return "redirect:/login";
        }

        Long cafeId = cafeService.createCafe(me.getId(), form, cafePhotoFile, bizDocFile);
        ra.addFlashAttribute("msg", "카페가 등록되었습니다. (승인 대기 중)");
        return "redirect:/cafes/" + cafeId;
    }

    /** 카페 상세 */
    @GetMapping("/{cafeId}")
    public String viewCafe(@PathVariable Long cafeId,
                           Authentication auth,
                           Model model,
                           RedirectAttributes ra) {

        // 0) 카페 존재 확인
        Cafe cafe = cafeService.findById(cafeId);
        if (cafe == null) throw new NotFoundException("카페가 없습니다.");

        // 1) 대표 사진
        CafePhoto mainPhoto = cafePhotoService.getMainPhoto(cafeId);

        // 2) 로그인 사용자 정보/권한
        String email = (auth != null ? auth.getName() : null);
        Long meId = null;
        boolean isAdmin = false;
        if (auth != null) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        if (email != null) {
            Member me = memberService.findByEmail(email);
            meId = (me != null ? me.getId() : null);
        }
        boolean isOwner = (meId != null && cafe.getOwner() != null
                && meId.equals(cafe.getOwner().getId()));

        // 3) 승인 전 접근 제한
        if (cafe.getStatus() != CafeStatus.APPROVED && !(isOwner || isAdmin)) {
            if (cafe.getStatus() == CafeStatus.PENDING) {
                ra.addFlashAttribute("flashInfo", "카페가 현재 검토 중입니다. 승인 후 열람할 수 있어요. (보통 수 분 소요)");
                return "redirect:/";
            } else {
                throw new NotFoundException("승인되지 않은 카페입니다.");
            }
        }

        // 4) 기본 모델
        model.addAttribute("cafe", cafe);
        model.addAttribute("mainPhoto", mainPhoto);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLoggedIn", auth != null && auth.isAuthenticated());

        // 5) CafeInfo 주입 (영업정보/소개/공지)
        var cafeInfoOpt = cafeInfoService.findByCafeId(cafeId);
        if (cafeInfoOpt.isPresent()) {
            var ci = cafeInfoOpt.get();
            model.addAttribute("info", true);
            model.addAttribute("cafeOpenTime",  ci.getOpenTime());
            model.addAttribute("cafeCloseTime", ci.getCloseTime());
            model.addAttribute("cafeHoliday",   ci.getHoliday());
            model.addAttribute("cafeNotice",    ci.getNotice());
            model.addAttribute("cafeInfo",      ci.getInfo());
        } else {
            model.addAttribute("info", false);
        }

        // 6) 사진 갤러리 목록
        var photoVm = cafePhotoService.list(cafeId).stream()
                .map(p -> Map.of("cafePhotoUrl", p.getUrl()))
                .collect(Collectors.toList());
        model.addAttribute("cafePhotos", photoVm);

        // 7) ✅ 메뉴(썸네일 포함) 주입
        var menus = menuService.findByCafeId(cafeId); // 프로젝트 메서드명에 맞게 사용
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.KOREA);

        var menuVm = menus.stream().map(m -> {
            String photo = null;

            // 엔티티/DTO에 따라 photo 필드명이 다를 수 있어 유연하게 추출
            try {
                var mm = m.getClass().getMethod("getPhotoUrl");
                Object v = mm.invoke(m);
                if (v != null) photo = String.valueOf(v);
            } catch (ReflectiveOperationException ignored) {}
            if (photo == null) {
                try {
                    var mm = m.getClass().getMethod("getPhoto");
                    Object v = mm.invoke(m);
                    if (v != null) photo = String.valueOf(v);
                } catch (ReflectiveOperationException ignored) {}
            }

            String priceText;
            try {
                var pm = m.getClass().getMethod("getPrice");
                Object pv = pm.invoke(m);
                long price = (pv instanceof Number) ? ((Number) pv).longValue() : Long.parseLong(String.valueOf(pv));
                priceText = nf.format(price) + "원";
            } catch (Exception e) {
                priceText = ""; // price가 없다면 빈값
            }

            String name;
            try {
                var nm = m.getClass().getMethod("getName");
                Object nv = nm.invoke(m);
                name = (nv != null ? String.valueOf(nv) : "");
            } catch (Exception e) {
                name = "";
            }

            return Map.of(
                    "menuName",  name,
                    "menuPrice", priceText,
                    "menuPhoto", (photo != null && !photo.isBlank()) ? photo : "/images/placeholder-cafe.jpg"
            );
        }).collect(Collectors.toList());
        model.addAttribute("menus", menuVm);

        // 8) 리뷰 목록 + 리뷰 사진
        var reviews = reviewService.findByCafeIdWithMember(cafeId);
        for (var r : reviews) {
            var photos = reviewPhotoService.findByReviewIdOrderBySortIndexAsc(r.getId());
            r.setPhotos(photos);
        }
        model.addAttribute("reviews", reviews);

        // 9) 좋아요/아쉬워요 + 태그 집계
        var stats = cafeStatsService.buildStats(cafeId, 12);
        model.addAttribute("cafeGood", stats.get("good"));
        model.addAttribute("cafeBad",  stats.get("bad"));
        model.addAttribute("cafeTags", stats.get("tags"));

        // 10) 즐겨찾기
        boolean isFavorited = (email != null) && favoriteService.isFavoritedByEmail(email, cafeId);
        long favoriteCount = favoriteService.countFavoriteForCafe(cafeId);
        model.addAttribute("isFavorited", isFavorited);
        model.addAttribute("favoriteCount", favoriteCount);

        return "cafes/detail";
    }
}
