// src/main/java/com/example/MyPickCafe/controller/CafeController.java
package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.*;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeCardForm;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.util.*;
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
    private final RecommendService recommendService;

    /** 카페 목록 */
    @GetMapping
    public String cafeList(@RequestParam(value = "sort", defaultValue = "views") String sort,
                           @RequestParam(value = "tag",  required = false) String tag,
                           Authentication auth,
                           Model model) {

        boolean isLoggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
        boolean needsLogin   = false;
        boolean noNeedsSet   = false;

        List<CafeCardForm> cafes;

        if ("recommend".equals(sort)) {
            if (!isLoggedIn) {
                needsLogin = true;
                cafes = cafeService.findApprovedCardsSorted("views", 40);
            } else {
                Member me = memberService.findByEmail(auth.getName());
                cafes = recommendService.recommendForMember(me.getId(), 40);
                if (cafes.isEmpty() && !recommendService.hasNeeds(me.getId())) noNeedsSet = true;
            }
        } else if (tag != null && !tag.isBlank()) {
            String[] parts = tag.split(":", 2);
            cafes = parts.length == 2
                    ? cafeService.findApprovedCardsByTag(parts[0], parts[1], 40)
                    : cafeService.findApprovedCardsSorted(sort, 40);
        } else {
            cafes = cafeService.findApprovedCardsSorted(sort, 40);
        }

        String tagSuffix  = (tag != null && !tag.isBlank()) ? "&tag=" + tag : "";
        String sortSuffix = "?sort=" + sort;

        model.addAttribute("cafes",       cafes);
        model.addAttribute("isEmpty",     cafes.isEmpty());
        model.addAttribute("sort",        sort);
        model.addAttribute("selectedTag", tag);
        model.addAttribute("isLoggedIn",  isLoggedIn);
        model.addAttribute("needsLogin",  needsLogin);
        model.addAttribute("noNeedsSet",  noNeedsSet);

        model.addAttribute("sortViews",     "views".equals(sort));
        model.addAttribute("sortLikes",     "likes".equals(sort));
        model.addAttribute("sortNewest",    "newest".equals(sort));
        model.addAttribute("sortRecommend", "recommend".equals(sort));

        model.addAttribute("urlSortViews",     "/cafes?sort=views"     + tagSuffix);
        model.addAttribute("urlSortLikes",     "/cafes?sort=likes"     + tagSuffix);
        model.addAttribute("urlSortNewest",    "/cafes?sort=newest"    + tagSuffix);
        model.addAttribute("urlSortRecommend", "/cafes?sort=recommend" + tagSuffix);
        model.addAttribute("urlClearTag",      "/cafes" + sortSuffix);
        model.addAttribute("hasTagFilter",     tag != null && !tag.isBlank());

        model.addAttribute("tagGroups", buildTagGroups(sort, tag));
        return "cafes/list";
    }

    private List<Map<String, Object>> buildTagGroups(String sort, String selectedTag) {
        List<Map<String, Object>> groups = new ArrayList<>();
        addTagGroup(groups, FacilityTag.values(), sort, selectedTag);
        addTagGroup(groups, MenuTag.values(),      sort, selectedTag);
        addTagGroup(groups, PurposeTag.values(),   sort, selectedTag);
        addTagGroup(groups, MoodTag.values(),      sort, selectedTag);
        return groups;
    }

    private void addTagGroup(List<Map<String, Object>> groups, TagEnum[] tags,
                             String sort, String selectedTag) {
        if (tags.length == 0) return;
        List<Map<String, Object>> items = new ArrayList<>();
        for (TagEnum t : tags) {
            String value  = t.getCategory() + ":" + t.name();
            boolean active = value.equals(selectedTag);
            Map<String, Object> item = new HashMap<>();
            item.put("label",  t.getLabel());
            item.put("active", active);
            item.put("href",   active
                    ? "/cafes?sort=" + sort
                    : "/cafes?sort=" + sort + "&tag=" + value);
            items.add(item);
        }
        Map<String, Object> group = new HashMap<>();
        group.put("categoryLabel", tags[0].getCategoryLabel());
        group.put("tags", items);
        groups.add(group);
    }

    /** 신규 등록 폼 */
    @PreAuthorize("hasAnyRole('CAFEOWNER','ADMIN')")
    @GetMapping("/new")
    public String createCafeForm() {
        return "cafes/create";
    }

    /** 카페 등록 */
    @PreAuthorize("hasAnyRole('CAFEOWNER','ADMIN')")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createCafe(Authentication auth,
                             @Valid @ModelAttribute("form") CafeForm form,
                             @RequestParam(value = "cafePhotoFiles", required = false) List<MultipartFile> cafePhotoFiles,
                             @RequestParam(value = "bizDocFile", required = false) MultipartFile bizDocFile,
                             RedirectAttributes ra) {

        String email = auth.getName();
        Member me = memberService.findByEmail(email);
        if (me == null) {
            ra.addFlashAttribute("errorMsg", "로그인 정보를 찾을 수 없습니다.");
            return "redirect:/login";
        }

        try {
            Long cafeId = cafeService.createCafe(me.getId(), form, cafePhotoFiles, bizDocFile);
            ra.addFlashAttribute("successMsg", "카페 등록 신청이 완료되었습니다. 사진과 메뉴를 추가해주세요.");
            return "redirect:/cafes/" + cafeId + "/manage";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/cafes/new";
        }
    }

    /** 카페 관리 페이지 (사진/메뉴 추가) */
    @PreAuthorize("hasAnyRole('CAFEOWNER','ADMIN')")
    @GetMapping("/{cafeId}/manage")
    public String manageCafe(@PathVariable Long cafeId, Authentication auth, Model model) {
        Cafe cafe = cafeService.findById(cafeId);

        // 오너 또는 관리자만 접근
        Member me = memberService.findByEmail(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isOwner = cafe.getOwner() != null && cafe.getOwner().getId().equals(me.getId());
        if (!isOwner && !isAdmin) {
            return "redirect:/cafes/" + cafeId;
        }

        var photos = cafePhotoService.list(cafeId).stream()
                .map(p -> Map.of("id", p.getId(), "url", p.getUrl(), "main", p.isMain()))
                .collect(Collectors.toList());

        var menus = menuService.findByCafeId(cafeId).stream()
                .map(m -> Map.of(
                        "id",    m.getId(),
                        "name",  m.getName(),
                        "price", m.getPrice(),
                        "photo", m.getPhoto() != null ? m.getPhoto() : "/images/placeholder-cafe.jpg"
                ))
                .collect(Collectors.toList());

        model.addAttribute("cafe",        cafe);
        model.addAttribute("cafePhotos",  photos);
        model.addAttribute("menus",       menus);
        model.addAttribute("isPending",   cafe.isPending());
        model.addAttribute("isApproved",  cafe.isApproved());
        model.addAttribute("isRejected",  cafe.isRejected());
        return "cafes/manage";
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
