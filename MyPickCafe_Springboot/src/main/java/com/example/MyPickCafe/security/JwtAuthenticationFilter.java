package com.example.MyPickCafe.security;

import com.example.MyPickCafe.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final MemberRepository memberRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService,
                                   MemberRepository memberRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            // 1) Authorization 헤더 or 2) HttpOnly 쿠키(AT) 에서 토큰 해석
            String token = resolveToken(req);

            if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = tokenProvider.extractUsername(token); // 유효하지 않으면 예외 발생 가능
                Number verClaim = tokenProvider.extractClaim(token, c -> c.get("ver", Number.class)); // null 가능

                if (email != null) {
                    var opt = memberRepository.findByEmail(email);
                    if (opt.isPresent()) {
                        long currentVer = opt.get().getTokenVersion() == null ? 0L : opt.get().getTokenVersion();
                        long tokenVer   = verClaim == null ? 0L : verClaim.longValue();

                        // 서버측 무효화(버전) 체크
                        if (currentVer != tokenVer) {
                            log.debug("JWT 버전 불일치: tokenVer={}, currentVer={} -> 인증 미설정", tokenVer, currentVer);
                            chain.doFilter(req, res);
                            return;
                        }

                        // UserDetails 로 권한 구성 후 SecurityContext 채움
                        UserDetails ud = userDetailsService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("JWT 인증 성공: {}", email);
                    }
                }
            }
        } catch (Exception e) {
            // 토큰 오류/만료 등은 인증 미설정으로 넘김 (익명으로 처리)
            log.debug("JWT 처리 실패: {}", e.getMessage());
        }
        chain.doFilter(req, res);
    }

    /**
     * Authorization: Bearer ... 우선, 없으면 쿠키 AT에서 추출
     */
    private String resolveToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (StringUtils.hasText(auth)) {
            // 대소문자 무시하여 "Bearer " 접두 확인
            if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = auth.substring(7).trim();
                if (StringUtils.hasText(token)) return token;
            }
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("AT".equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
