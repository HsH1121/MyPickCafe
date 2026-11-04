package com.example.MyPickCafe.api;

import com.example.MyPickCafe.dto.MemberForm;
import com.example.MyPickCafe.dto.MemberResponse;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.LoginService;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final MemberService memberService;
    private final LoginService loginService;
    private final JwtTokenProvider jwtTokenProvider;

    /** 인증 쿠키에 Secure 속성을 붙일지 여부. dev=false(HTTP), prod=true(HTTPS 전용). */
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    public AuthApiController(MemberService memberService,
                             LoginService loginService,
                             JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.loginService = loginService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberForm body, HttpServletRequest request) {
        String email = body.getEmail();
        String pw = body.getPassword();
        if (email == null || email.isBlank() || pw == null || pw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이메일과 비밀번호를 입력해주세요."));
        }

        // 없는 이메일과 틀린 비밀번호는 같은 응답(401)을 준다.
        LoginService.Result result = loginService.authenticate(email, pw, request.getRemoteAddr());
        if (result.status() == LoginService.Status.BLOCKED) {
            long seconds = Math.max(1, result.retryAfter().toSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                    .body(Map.of("message", "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요."));
        }
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."));
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
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtTokenProvider.getExpiration())
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "회원을 찾을 수 없습니다."));
        }
        // 입력 폼 DTO(MemberForm)를 응답에 재사용하면 tokenVersion·password 키가 함께 나간다.
        return ResponseEntity.ok(MemberResponse.from(memberOpt.get()));
    }
}
