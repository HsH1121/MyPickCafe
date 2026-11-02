package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.LoginService;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.security.JwtTokenProvider;
import com.example.MyPickCafe.security.PasswordPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Controller
public class AuthController {

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginService loginService;
    private final PasswordPolicy passwordPolicy;

    public AuthController(MemberService memberService,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          LoginService loginService,
                          PasswordPolicy passwordPolicy) {
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginService = loginService;
        this.passwordPolicy = passwordPolicy;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value="error", required=false) String error,
                            @RequestParam(value="redirect", required=false) String redirect,
                            Model model) {
        if (error != null && !error.isBlank()) model.addAttribute("error", error);
        if (redirect != null && !redirect.isBlank()) model.addAttribute("redirect", redirect);
        if (!model.containsAttribute("prefillEmail")) model.addAttribute("prefillEmail", "");
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam("memberEmail") String email,
                              @RequestParam("memberPassword") String password,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              RedirectAttributes ra,
                              Model model) {
        // 실패 시 입력한 이메일만 다시 채운다. 비밀번호는 플래시(세션)에 담거나 HTML 에 다시 쓰지 않는다.
        LoginService.Result result = loginService.authenticate(email, password, request.getRemoteAddr());
        if (result.status() == LoginService.Status.BLOCKED) {
            long minutes = Math.max(1, (result.retryAfter().toSeconds() + 59) / 60);
            ra.addFlashAttribute("error", "로그인 시도가 너무 많습니다. " + minutes + "분 후 다시 시도해주세요.");
            ra.addFlashAttribute("prefillEmail", email);
            return "redirect:/login";
        }
        if (!result.isSuccess()) {
            // 없는 이메일과 틀린 비밀번호는 같은 문구
            ra.addFlashAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            ra.addFlashAttribute("prefillEmail", email);
            return "redirect:/login";
        }
        Member member = result.member();

        String roleName = member.getRoleKind() == null ? "MEMBER" : member.getRoleKind().name();
        var ud = User
                .withUsername(member.getEmail())
                .password(member.getPassword())
                .authorities("ROLE_" + roleName)
                .build();

        String token = jwtTokenProvider.generateToken(ud, member.getTokenVersion());
        ResponseCookie cookie = ResponseCookie.from("AT", token)
                .httpOnly(true).secure(cookieSecure).sameSite("Lax")
                .path("/").maxAge(jwtTokenProvider.getExpiration())
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
        try {
            passwordPolicy.validate(memberPassword, memberEmail, memberNickname);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
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
