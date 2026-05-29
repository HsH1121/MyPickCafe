package com.example.MyPickCafe.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class HtmlAwareAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            String msg = URLEncoder.encode("권한이 없습니다.", StandardCharsets.UTF_8);
            res.sendRedirect("/login?error=" + msg);
        } else {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN");
        }
    }
}
