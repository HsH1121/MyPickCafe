package com.example.MyPickCafe.security;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 비밀번호를 설정하는 경로(회원가입·비밀번호 변경·관리자 생성)와 로그인 호환성을 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("비밀번호 설정 경로")
class PasswordFlowIntegrationTest {

    private static final String STRONG = "otter & <lamp> 42";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member saveMember(String email, String nickname, String hash) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(hash);
        m.setNickname(nickname);
        m.setRoleKind(RoleKind.MEMBER);
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signup(String email, String pw) {
        return post("/signup")
                .param("email", email).param("password", pw).param("passwordConfirm", pw)
                .param("nickname", "signup" + Math.abs(email.hashCode() % 10000))
                .param("age", "25").param("gender", "M");
    }

    private String apiLogin(String email, String pw) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + pw.replace("\"", "\\\"") + "\"}";
        return String.valueOf(mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus());
    }

    @Test
    @DisplayName("회원가입: 정책 위반(짧음·흔함)은 가입되지 않는다")
    void signupRejectsWeakPasswords() throws Exception {
        mvc.perform(signup("short@example.com", "sh-pw-1"))
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attribute("error", "비밀번호는 8자 이상이어야 합니다."));
        mvc.perform(signup("common@example.com", "Password1234"))
                .andExpect(view().name("auth/signup"));
        assertThat(memberRepository.findByEmail("short@example.com")).isEmpty();
        assertThat(memberRepository.findByEmail("common@example.com")).isEmpty();
    }

    @Test
    @DisplayName("회원가입: 특수문자·공백이 든 비밀번호를 입력한 그대로 저장해 폼·API 로그인이 모두 된다")
    void signupStoresRawPassword() throws Exception {
        mvc.perform(signup("raw@example.com", STRONG)).andExpect(redirectedUrl("/login"));

        Member saved = memberRepository.findByEmail("raw@example.com").orElseThrow();
        assertThat(new BCryptPasswordEncoder().matches(STRONG, saved.getPassword()))
                .as("XSS 필터가 & < > 를 바꾸지 않고 원문을 해시해야 한다").isTrue();

        assertThat(apiLogin("raw@example.com", STRONG)).isEqualTo("200");
        mvc.perform(post("/login").param("memberEmail", "raw@example.com").param("memberPassword", STRONG))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("정책 이전의 짧은 비밀번호 회원도 로그인은 막지 않는다")
    void existingShortPasswordStillLogsIn() throws Exception {
        saveMember("old@example.com", "oldie", passwordEncoder.encode("1234"));
        assertThat(apiLogin("old@example.com", "1234")).isEqualTo("200");
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호가 틀리면 바뀌지 않는다")
    void changeRequiresCurrentPassword() throws Exception {
        saveMember("chg1@example.com", "chg1", passwordEncoder.encode("old password 123"));

        mvc.perform(post("/member/edit").with(user("chg1@example.com").roles("MEMBER"))
                        .param("current_password", "not my password")
                        .param("new_password", "brand new phrase 7"))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attribute("pwError", "현재 비밀번호가 일치하지 않습니다."));

        Member m = memberRepository.findByEmail("chg1@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("old password 123", m.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호 변경: 새 비밀번호도 정책을 지켜야 한다")
    void changeEnforcesPolicy() throws Exception {
        saveMember("chg2@example.com", "chg2", passwordEncoder.encode("old password 123"));
        mvc.perform(post("/member/edit").with(user("chg2@example.com").roles("MEMBER"))
                        .param("current_password", "old password 123")
                        .param("new_password", "qwerty123456"))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attributeExists("pwError"));
    }

    @Test
    @DisplayName("비밀번호 변경: 성공하면 기존 JWT 가 모두 무효가 되고 AT 쿠키를 지운다")
    void changeInvalidatesExistingTokens() throws Exception {
        saveMember("chg3@example.com", "chg3", passwordEncoder.encode("old password 123"));

        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"chg3@example.com\",\"password\":\"old password 123\"}")).andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        MvcResult change = mvc.perform(post("/member/edit").cookie(new Cookie("AT", token))
                        .param("current_password", "old password 123")
                        .param("new_password", "brand new phrase 7")
                        .param("new_password_confirm", "brand new phrase 7"))
                .andExpect(redirectedUrl("/login?password=changed"))
                .andReturn();
        assertThat(change.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(c -> c.startsWith("AT=;") && c.contains("Max-Age=0"));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(apiLogin("chg3@example.com", "brand new phrase 7")).isEqualTo("200");
        assertThat(apiLogin("chg3@example.com", "old password 123")).isEqualTo("401");
    }

    @Test
    @DisplayName("관리자 회원 생성 API 도 같은 정책을 쓰고, 위반은 400 이다")
    void adminCreateUsesPolicy() throws Exception {
        mvc.perform(post("/api/members").with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"made@example.com\",\"password\":\"short\",\"nickname\":\"made\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/members").with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"made@example.com\",\"password\":\"long enough phrase 9\",\"nickname\":\"made\"}"))
                .andExpect(status().isCreated());
    }
}
