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

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * 폼 로그인({@code POST /login}) 비밀번호 검증 회귀 테스트.
 *
 * <p>수정 전에는 BCrypt 비교가 실패하면 입력값과 저장값을 평문으로 다시 비교했다.
 * 주석은 "dev only"였지만 프로파일 구분 없이 동작해서, 평문 비밀번호가 저장된
 * 계정은 그 평문으로 로그인할 수 있었다. REST 로그인({@code /api/auth/login})에서는
 * 이미 제거된 폴백이 폼 로그인에만 남아 있던 것이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FormLoginTest {

    private static final String PASSWORD = "correct-password";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private void saveMember(String email, String storedPassword) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(storedPassword);
        m.setNickname(email.substring(0, email.indexOf('@')));
        m.setRoleKind(RoleKind.MEMBER);
        m.setTokenVersion(0L);
        memberRepository.save(m);
    }

    @Test
    @DisplayName("평문 비밀번호가 저장된 계정은 그 평문으로 로그인할 수 없다")
    void rejectsPlaintextStoredPassword() throws Exception {
        saveMember("plain@example.com", PASSWORD);

        mvc.perform(post("/login")
                        .param("memberEmail", "plain@example.com")
                        .param("memberPassword", PASSWORD))
                .andExpect(redirectedUrl("/login"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("BCrypt 해시가 저장된 계정은 올바른 비밀번호로 로그인하고 AT 쿠키를 받는다")
    void acceptsCorrectPasswordForHashedAccount() throws Exception {
        saveMember("hashed@example.com", passwordEncoder.encode(PASSWORD));

        mvc.perform(post("/login")
                        .param("memberEmail", "hashed@example.com")
                        .param("memberPassword", PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, startsWith("AT=")));
    }

    @Test
    @DisplayName("틀린 비밀번호로는 로그인할 수 없다")
    void rejectsWrongPassword() throws Exception {
        saveMember("wrong@example.com", passwordEncoder.encode(PASSWORD));

        mvc.perform(post("/login")
                        .param("memberEmail", "wrong@example.com")
                        .param("memberPassword", "wrong-password"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }
}
