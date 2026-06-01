package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.security.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class AuthController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(MemberService memberService,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value="error", required=false) String error,
                            @RequestParam(value="redirect", required=false) String redirect,
                            Model model) {
        if (error != null && !error.isBlank()) model.addAttribute("error", error);
        if (redirect != null && !redirect.isBlank()) model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam("memberEmail") String email,
                              @RequestParam("memberPassword") String password,
                              HttpServletResponse response,
                              RedirectAttributes ra,
                              Model model) {
        Optional<Member> memberTryingLogin = memberService.findByEmailOptional(email);
        if (memberTryingLogin.isEmpty()) {
            ra.addFlashAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            ra.addFlashAttribute("prefillEmail", email);
            ra.addFlashAttribute("prefillPassword", password);
            return "redirect:/login";
        }
        Member member = memberTryingLogin.get();
        boolean matches = false;
        if (member.getPassword() != null) {
            try { matches = passwordEncoder.matches(password, member.getPassword()); } catch (Exception ignored) {}
            if (!matches) matches = password.equals(member.getPassword()); // dev only
        }
        if (!matches) {
            ra.addFlashAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            ra.addFlashAttribute("prefillEmail", email);
            ra.addFlashAttribute("prefillPassword", password);
            return "redirect:/login";
        }

        String roleName = member.getRoleKind() == null ? "MEMBER" : member.getRoleKind().name();
        var ud = User
                .withUsername(member.getEmail())
                .password(member.getPassword())
                .authorities("ROLE_" + roleName)
                .build();

        String token = jwtTokenProvider.generateToken(ud, member.getTokenVersion()==null?0L:member.getTokenVersion());
        ResponseCookie cookie = ResponseCookie.from("AT", token)
                .httpOnly(true).secure(false).sameSite("Lax")
                .path("/").maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return "redirect:/";
    }

    @GetMapping("/signup")
    public String signupPage() { return "auth/signup"; }

    @PostMapping("/signup")
    public String signupSubmit(
            @RequestParam("email") String memberEmail,
            @RequestParam("password") String memberPassword,
            @RequestParam("passwordConfirm") String memberPasswordConfirm,
            @RequestParam("nickname") String memberNickname,
            @RequestParam("age") Integer memberAge,
            @RequestParam("gender") String memberGender,
            Model model) {

        if (!memberPassword.equals(memberPasswordConfirm)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "auth/signup";
        }
        if (memberService.existsByEmail(memberEmail)) {
            model.addAttribute("error", "이미 사용 중인 이메일입니다.");
            return "auth/signup";
        }
        if (memberService.existsByNickname(memberNickname)) {
            model.addAttribute("error", "이미 사용 중인 닉네임입니다.");
            return "auth/signup";
        }

        Member m = new Member();
        m.setEmail(memberEmail);
        m.setPassword(passwordEncoder.encode(memberPassword));
        m.setNickname(memberNickname);
        m.setAge(memberAge == null ? null : memberAge.longValue());
        m.setGender(("M".equalsIgnoreCase(memberGender) || "F".equalsIgnoreCase(memberGender)) ? memberGender.toUpperCase() : null);
        m.setRoleKind(RoleKind.MEMBER);
        m.setCreatedAt(LocalDateTime.now());
        m.setTokenVersion(0L);

        memberService.save(m);
        return "redirect:/login";
    }

    private String ensureUniqueNickname(String base) {
        String candidate = base;
        int counter = 1;
        while (memberService.existsByNickname(candidate)) {
            if (candidate.length() > 6) {
                candidate = base.substring(0, 6) + counter;
            } else {
                candidate = base + counter;
            }
            counter++;
        }
        return candidate;
    }
}
