package com.example.MyPickCafe.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class XssSanitizingFilter implements Filter {

    /**
     * 정리하지 않고 그대로 넘기는 비밀번호 파라미터.
     * 비밀번호는 화면에 다시 출력하지 않고 해시만 하므로 XSS 대상이 아니다. 정리하면 {@code & < >}·공백이
     * 바뀌어 사용자가 입력한 것과 다른 값이 해시되고, JSON 으로 받는 /api/auth/login 과 결과가 달라진다.
     */
    static final Set<String> RAW_PARAMETERS = Set.of(
            "password", "passwordConfirm", "memberPassword",
            "current_password", "new_password", "new_password_confirm"
    );

    private static String sanitize(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, XssRequestWrapper.SAFE);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest http) {
            chain.doFilter(new XssRequestWrapper(http), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    static class XssRequestWrapper extends HttpServletRequestWrapper {
        private static final Safelist SAFE = Safelist.basic();

        public XssRequestWrapper(HttpServletRequest request) { super(request); }

        @Override
        public String getParameter(String name) {
            String v = super.getParameter(name);
            return RAW_PARAMETERS.contains(name) ? v : sanitize(v);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            if (RAW_PARAMETERS.contains(name)) return values;
            String[] out = new String[values.length];
            for (int i = 0; i < values.length; i++) out[i] = sanitize(values[i]);
            return out;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = new HashMap<>(super.getParameterMap());
            map.replaceAll((k, v) -> {
                if (v == null || RAW_PARAMETERS.contains(k)) return v;
                String[] out = new String[v.length];
                for (int i = 0; i < v.length; i++) out[i] = sanitize(v[i]);
                return out;
            });
            return map;
        }
    }
}
