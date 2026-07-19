// src/main/java/com/example/MyPickCafe/security/SecurityConfig.java
package com.example.MyPickCafe.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer; // ✅ 추가
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final XssSanitizingFilter xssSanitizingFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsService userDetailsService,
            XssSanitizingFilter xssSanitizingFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.xssSanitizingFilter = xssSanitizingFilter;
    }

    // ✅ 최소수정 ②: 정적 리소스는 보안 필터 체인 자체에서 제외 (JWT/XSS도 안탐)
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/favicon.ico",
                "/webjars/**",
                "/css/**", "/js/**",
                "/images/**", "/img/**",
                "/uploads/**", "/files/**"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // ✅ CSP: 인라인 스크립트 금지(외부 파일만), 인라인 스타일은 허용
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        // JS SDK
                                        "script-src 'self' https://dapi.kakao.com https://t1.daumcdn.net 'unsafe-inline'; " +
                                        // ✅ 최소수정 ①: 모든 https 이미지 허용 (self/data/blob 포함)
                                        "img-src 'self' data: blob: https:; " +
                                        // 지오코딩/클러스터 등 XHR 대비
                                        "connect-src 'self' https://dapi.kakao.com https://*.daumcdn.net; " +
                                        // (필요 시) 폰트/스타일
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "font-src 'self' https:; " +
                                        "base-uri 'self'; frame-ancestors 'self'; object-src 'none'; " +
                                        "upgrade-insecure-requests;"
                        ))
                        .frameOptions(fo -> fo.sameOrigin())
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
                        .contentTypeOptions(Customizer.withDefaults())
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            if (isApiRequest(req)) {
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            } else {
                                res.sendRedirect("/login");
                            }
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            if (isApiRequest(req)) {
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                            } else {
                                res.sendRedirect("/");
                            }
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 비로그인 접근 허용
                        .requestMatchers("/", "/index/**", "/search/**", "/signup", "/login").permitAll()
                        .requestMatchers("/cafes", "/cafes/{cafeId}").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**", "/favicon.ico").permitAll()
                        .requestMatchers("/uploads/**", "/files/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chatbot/**").permitAll()

                        // ⚠️ 개인정보는 아래 "GET /api/** permitAll"보다 반드시 먼저
                        //    선언해야 한다 (Security는 최초 매칭 규칙이 이김).
                        .requestMatchers("/api/members/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/cafes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                        // ADMIN 전용
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // CAFEOWNER 전용 (카페 등록·사진·메뉴 관리)
                        .requestMatchers("/cafes/new", "/cafes/create").hasAnyRole("CAFEOWNER", "ADMIN")
                        .requestMatchers("/api/cafes/*/photos", "/api/cafes/photos/**").hasAnyRole("CAFEOWNER", "ADMIN")
                        .requestMatchers("/api/menus/**").hasAnyRole("CAFEOWNER", "ADMIN")

                        // 로그인 사용자 공통 (MEMBER, CAFEOWNER, ADMIN)
                        .requestMatchers("/reviews/**").authenticated()
                        .requestMatchers("/favorites/**").authenticated()
                        .requestMatchers("/member/**").authenticated()
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/api/private/**").authenticated()

                        .anyRequest().authenticated()
                )
                // 🔧 필터 순서: XSS -> JWT -> 나머지
                .addFilterBefore(xssSanitizingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 허용 오리진은 프로파일별로 주입한다.
        // dev: "*" (로컬 프론트엔드 포트가 매번 달라짐)
        // prod: 명시적 도메인 목록 (CORS_ALLOWED_ORIGINS 환경변수)
        config.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
        );
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private boolean isApiRequest(jakarta.servlet.http.HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri.startsWith("/api/")) return true;
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}
