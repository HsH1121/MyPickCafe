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

@Component
public class XssSanitizingFilter implements Filter {

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
        public String getParameter(String name) { return sanitize(super.getParameter(name)); }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] out = new String[values.length];
            for (int i = 0; i < values.length; i++) out[i] = sanitize(values[i]);
            return out;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = new HashMap<>(super.getParameterMap());
            map.replaceAll((k, v) -> {
                if (v == null) return null;
                String[] out = new String[v.length];
                for (int i = 0; i < v.length; i++) out[i] = sanitize(v[i]);
                return out;
            });
            return map;
        }

        private String sanitize(String input) {
            if (input == null) return null;
            return Jsoup.clean(input, SAFE);
        }
    }
}
