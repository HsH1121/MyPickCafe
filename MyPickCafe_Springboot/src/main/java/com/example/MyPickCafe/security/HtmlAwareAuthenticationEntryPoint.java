package com.example.MyPickCafe.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class HtmlAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        String accept = req.getHeader("Accept");
        boolean wantsHtml = accept != null && accept.contains("text/html");
        if (wantsHtml) {
            String back = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
            String redirect = URLEncoder.encode(back, StandardCharsets.UTF_8);
            String msg = URLEncoder.encode("로그인이 필요한 서비스입니다.", StandardCharsets.UTF_8);
            res.sendRedirect("/login?error=" + msg + "&redirect=" + redirect);
        } else {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"message\":\"UNAUTHORIZED\",\"detail\":\"로그인이 필요한 서비스입니다.\"}");
        }
    }
}
