package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.FacilityTag;
import com.example.MyPickCafe.domain.MenuTag;
import com.example.MyPickCafe.domain.MoodTag;
import com.example.MyPickCafe.domain.PurposeTag;
import com.example.MyPickCafe.domain.TagEnum;
import com.example.MyPickCafe.dto.MemberForm;
import com.example.MyPickCafe.dto.MyReviewItem;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.UserNeeds;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.service.NeedsService;
import com.example.MyPickCafe.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final ReviewService reviewService;
    private final NeedsService needsService;

    /* =========================
     *        MY PAGE
     * ========================= */
    @GetMapping("/me")
    public String myPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                         @RequestParam(value = "needs", required = false) String needsParam,
                         Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login";

        MemberForm view = toForm(member);

        populateCommonUserModel(model, member);

        // 본문 모델
        model.addAttribute("member", view);
        model.addAttribute("memberPhoto", member.getPhoto() == null ? "" : member.getPhoto());

        model.addAttribute("needs_saved", "saved".equals(needsParam));

        // 니즈 선택 패널
        List<UserNeeds> myNeeds = needsService.findByMemberId(member.getId());
        Set<String> selected = myNeeds.stream()
                .map(n -> n.getCategoryCode() + ":" + n.getCode())
                .collect(Collectors.toSet());
        model.addAttribute("needsCategories", buildNeedsCategories(selected));
        model.addAttribute("hasSelectedNeeds", !myNeeds.isEmpty());

        // 좌측 네비 활성화
        model.addAttribute("nav_me", true);
        model.addAttribute("isMemberRole", member.getRoleKind() == com.example.MyPickCafe.domain.RoleKind.MEMBER);

        return "member/mypage";
    }

    /* =========================
     *     PROFILE EDIT (GET)
     * ========================= */
    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                       Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login";

        MemberForm view = toForm(member);

        // 공통 키
        populateCommonUserModel(model, member);

        // 사진 파생 값
        String photoName = member.getPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("hasPhoto", hasPhoto);

        // 본문 모델
        model.addAttribute("member", view);

        // 네비
        model.addAttribute("nav_edit", true);

        return "member/edit";
    }

    /* =========================
     *     PROFILE EDIT (POST)
     * ========================= */
    @PostMapping("/edit")
    public String editDo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                         @RequestParam(value = "member_nickname", required = false) String nickname,
                         @RequestParam(value = "member_age", required = false) Long age,
                         @RequestParam(value = "member_gender", required = false) String gender,
                         @RequestParam(value = "member_photo", required = false) String photo,
                         @RequestParam(value = "current_password", required = false) String currentPassword,
                         @RequestParam(value = "new_password", required = false) String newPassword,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        boolean pwChanged = memberService.updateSelf(
                me.getId(), nickname, age, gender, photo, currentPassword, newPassword
        );

        if (pwChanged) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, auth);
            return "redirect:/login?password=changed";
        }
        return "redirect:/member/me?update=success";
    }

    /* =========================
     *       WITHDRAW (GET)
     * ========================= */
    @GetMapping("/withdraw")
    public String withdrawPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                               Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 공통 키
        populateCommonUserModel(model, me);

        // 점표기 회피용 단일 키
        model.addAttribute("email", me.getEmail());
        model.addAttribute("nickname", me.getNickname());

        // 네비
        model.addAttribute("nav_withdraw", true);

        return "member/withdraw";
    }

    /* =========================
     *       WITHDRAW (POST)
     * ========================= */
    @PostMapping("/withdraw")
    public String withdrawDo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        memberService.withdrawSelf(me.getId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        return "redirect:/?withdraw=success";
    }

    /* =========================
     *        MY REVIEWS
     * ========================= */
    @GetMapping("/reviews")
    public String myReviews(@AuthenticationPrincipal User principal,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        Page<MyReviewItem> myReviews = reviewService.findMyReviews(me.getId(), PageRequest.of(page, 10));

        // 공통 키
        populateCommonUserModel(model, me);

        model.addAttribute("items", myReviews.getContent());
        model.addAttribute("hasContent", !myReviews.isEmpty());
        model.addAttribute("totalElements", myReviews.getTotalElements());

        // 페이지네이션
        int current = myReviews.getNumber();
        int totalPages = Math.max(myReviews.getTotalPages(), 1);
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("display", i + 1);
            m.put("active", i == current);
            pages.add(m);
        }

        model.addAttribute("pages", pages);
        model.addAttribute("hasPrev", myReviews.hasPrevious());
        model.addAttribute("hasNext", myReviews.hasNext());
        model.addAttribute("prevPage", Math.max(0, current - 1));
        model.addAttribute("nextPage", Math.min(totalPages - 1, current + 1));

        // 네비
        model.addAttribute("nav_reviews", true);

        return "member/reviews";
    }

    /* =========================
     *       MY NEEDS (POST)
     * ========================= */
    @PostMapping("/needs")
    public String saveNeeds(@AuthenticationPrincipal User principal,
                            @RequestParam(value = "needs", required = false) List<String> needs) {
        if (principal == null) return "redirect:/login";
        Member me = memberService.findByEmail(principal.getUsername());
        needsService.replaceAll(me.getId(), needs);
        return "redirect:/member/me?needs=saved";
    }

    /* =========================
     *         HELPERS
     * ========================= */

    private MemberForm toForm(Member member) {
        MemberForm view = new MemberForm();
        view.setId(member.getId());
        view.setEmail(member.getEmail());
        view.setNickname(member.getNickname());
        view.setAge(member.getAge());
        view.setGender(member.getGender());
        view.setRoleKind(member.getRoleKind() != null ? member.getRoleKind().name() : null);
        view.setCreatedAt(member.getCreatedAt());
        view.setPhoto(member.getPhoto());
        view.setTokenVersion(member.getTokenVersion());
        return view;
    }

    private List<Map<String, Object>> buildNeedsCategories(Set<String> selected) {
        List<Map<String, Object>> categories = new ArrayList<>();
        addNeedsCategory(categories, FacilityTag.values(), selected);
        addNeedsCategory(categories, MenuTag.values(), selected);
        addNeedsCategory(categories, PurposeTag.values(), selected);
        addNeedsCategory(categories, MoodTag.values(), selected);
        return categories;
    }

    private void addNeedsCategory(List<Map<String, Object>> categories, TagEnum[] tags, Set<String> selected) {
        if (tags.length == 0) return;
        List<Map<String, Object>> items = Arrays.stream(tags)
                .map(t -> needsItem(t.getCategory() + ":" + t.name(), t.getLabel(),
                        selected.contains(t.getCategory() + ":" + t.name())))
                .collect(Collectors.toList());
        categories.add(needsCategory(tags[0].getCategoryLabel(), items));
    }

    private Map<String, Object> needsCategory(String label, List<Map<String, Object>> items) {
        Map<String, Object> m = new HashMap<>();
        m.put("categoryLabel", label);
        m.put("items", items);
        return m;
    }

    private Map<String, Object> needsItem(String value, String label, boolean selected) {
        Map<String, Object> m = new HashMap<>();
        m.put("value", value);
        m.put("label", label);
        m.put("selected", selected);
        return m;
    }

    private void populateCommonUserModel(Model model, Member member) {
        String photoName = member.getPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("memberNickname", member.getNickname());
        model.addAttribute("currentUserNickname", member.getNickname());
        model.addAttribute("hasPhoto", hasPhoto);
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("memberPhoto", hasPhoto ? photoName : "");
    }
}
