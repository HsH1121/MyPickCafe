package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.dto.MemberForm;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** 인증 쿠키에 Secure 속성을 붙일지 여부. dev=false(HTTP), prod=true(HTTPS 전용). */
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    public AuthApiController(MemberService memberService,
                             PasswordEncoder passwordEncoder,
                             JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberForm body) {
        String email = body.getEmail();
        String pw = body.getPassword();
        if (email == null || email.isBlank() || pw == null || pw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email/password required"));
        }

        Optional<Member> memberOpt = memberService.findByEmailOptional(email);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials"));
        }
        Member member = memberOpt.get();

        // 저장된 해시와만 비교한다.
        // (과거에는 해시 불일치 시 평문 비교로 폴백했는데, 평문 비밀번호가 저장된
        //  계정이 하나라도 생기면 그대로 로그인이 뚫리므로 제거했다.)
        boolean matches = false;
        if (member.getPassword() != null) {
            try {
                matches = passwordEncoder.matches(pw, member.getPassword());
            } catch (IllegalArgumentException e) {
                // 해시 형식이 아닌 값이 저장된 경우 — 인증 실패로 처리
                matches = false;
            }
        }
        if (!matches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials"));
        }

        String roleName = member.getRoleKind() == null ? "MEMBER" : member.getRoleKind().name();
        var ud = User
                .withUsername(member.getEmail())
                .password(member.getPassword())
                .authorities("ROLE_" + roleName)
                .build();

        String token = jwtTokenProvider.generateToken(ud, member.getTokenVersion());

        ResponseCookie cookie = ResponseCookie.from("AT", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("tokenType", "Bearer", "token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication auth) {
        ResponseCookie del = ResponseCookie.from("AT", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, del.toString())
                    .build();
        }

        boolean bumped = memberService.bumpTokenVersion(auth.getName());
        if (bumped) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, del.toString())
                    .build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.SET_COOKIE, del.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Member> memberOpt = memberService.findByEmailOptional(auth.getName());
        if (memberOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        Member m = memberOpt.get();
        MemberForm out = new MemberForm();
        out.setId(m.getId());
        out.setEmail(m.getEmail());
        out.setNickname(m.getNickname());
        out.setAge(m.getAge());
        out.setGender(m.getGender());
        out.setRoleKind(m.getRoleKind() == null ? null : m.getRoleKind().name());
        out.setCreatedAt(m.getCreatedAt());
        out.setPhoto(m.getPhoto());
        out.setTokenVersion(m.getTokenVersion());
        return ResponseEntity.ok(out);
    }
}
