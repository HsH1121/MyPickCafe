package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * 폼 로그인({@code POST /login})이 발급하는 AT 쿠키가 {@code app.cookie.secure} 설정을 따르는지 확인한다.
 *
 * <p>수정 전에는 폼 로그인만 {@code secure(false)}로 고정돼 있어서, prod(HTTPS) 설정에서도
 * JWT 쿠키가 Secure 없이 나갔다. REST 로그인({@code /api/auth/login})은 이미 설정값을 따른다.
 */
@SpringBootTest(properties = "app.cookie.secure=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FormLoginSecureCookieTest {

    private static final String PASSWORD = "correct-password";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("app.cookie.secure=true 이면 폼 로그인 AT 쿠키에 Secure 가 붙는다")
    void formLoginCookieIsSecureWhenConfigured() throws Exception {
        Member m = new Member();
        m.setEmail("secure@example.com");
        m.setPassword(passwordEncoder.encode(PASSWORD));
        m.setNickname("secure");
        m.setRoleKind(RoleKind.MEMBER);
        m.setTokenVersion(0L);
        memberRepository.save(m);

        mvc.perform(post("/login")
                        .param("memberEmail", "secure@example.com")
                        .param("memberPassword", PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")));
    }
}
