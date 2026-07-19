package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.domain.RoleKind;
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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody MemberForm body) {
        String email = body.getEmail();
        String pw = body.getPassword();
        if (email == null || email.isBlank() || pw == null || pw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email/password required"));
        }
        if (memberService.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already exists"));
        }

        Member m = new Member();
        m.setEmail(email);
        m.setPassword(passwordEncoder.encode(pw));
        m.setNickname(body.getNickname());
        m.setAge(body.getAge());
        m.setGender(body.getGender());
        RoleKind rk = RoleKind.MEMBER;
        if (body.getRoleKind() != null && !body.getRoleKind().isBlank()) {
            try { rk = RoleKind.valueOf(body.getRoleKind().toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        m.setRoleKind(rk);
        m.setPhoto(body.getPhoto());
        m.setTokenVersion(0L);

        memberService.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).build();
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

        boolean matches = false;
        if (member.getPassword() != null) {
            try { matches = passwordEncoder.matches(pw, member.getPassword()); } catch (Exception ignored) {}
            if (!matches) matches = pw.equals(member.getPassword()); // dev only
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
