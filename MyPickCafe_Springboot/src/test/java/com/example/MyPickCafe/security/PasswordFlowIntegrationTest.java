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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 비밀번호를 설정하는 경로(회원가입·비밀번호 변경·관리자 생성)와 로그인 호환성을 확인한다.
 * 공백이 든 비밀번호는 기존 회원에게도 없다고 보고, 설정·로그인·현재 비밀번호 확인 모두에서 거절한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("비밀번호 설정 경로")
class PasswordFlowIntegrationTest {

    private static final String STRONG = "otter&<lamp>42";

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
    @DisplayName("회원가입: 공백이 든 비밀번호는 가입되지 않는다")
    void signupRejectsWhitespace() throws Exception {
        mvc.perform(signup("space@example.com", "otter lamp 42"))
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attribute("error", "비밀번호에는 공백을 쓸 수 없습니다."));
        assertThat(memberRepository.findByEmail("space@example.com")).isEmpty();
    }

    @Test
    @DisplayName("API 로그인: 공백이 든 비밀번호는 해시 비교 전에 400 으로 거절한다(저장된 해시와 같아도)")
    void apiLoginRejectsWhitespace() throws Exception {
        // 정책상 생길 수 없는 값이지만, 해시 비교 전에 막는지 보려고 공백 비밀번호 해시를 직접 넣어 둔다.
        saveMember("spaced@example.com", "spaced", passwordEncoder.encode("otter lamp 42"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"spaced@example.com\",\"password\":\"otter lamp 42\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호에는 공백을 쓸 수 없습니다."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"spaced@example.com\",\"password\":\"otterlamp42\\u3000\"}"))
                .andExpect(status().isBadRequest());
        // 없는 이메일도 같은 응답 — 가입 여부가 드러나지 않는다
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"otter lamp 42\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호에는 공백을 쓸 수 없습니다."));
    }

    @Test
    @DisplayName("API 로그인: 빈 비밀번호는 '입력해주세요', 공백만 보낸 비밀번호는 공백 금지로 400")
    void apiLoginEmptyVsBlankPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일과 비밀번호를 입력해주세요."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\",\"password\":\"    \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호에는 공백을 쓸 수 없습니다."));
    }

    @Test
    @DisplayName("폼 로그인: 공백이 든 비밀번호는 거절하고 이메일만 다시 채운다")
    void formLoginRejectsWhitespace() throws Exception {
        saveMember("spaced2@example.com", "spaced2", passwordEncoder.encode("otter lamp 42"));
        mvc.perform(post("/login").param("memberEmail", "spaced2@example.com").param("memberPassword", "otter lamp 42"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "비밀번호에는 공백을 쓸 수 없습니다."))
                .andExpect(flash().attribute("prefillEmail", "spaced2@example.com"));
        mvc.perform(post("/login").param("memberEmail", "spaced2@example.com").param("memberPassword", " otterlamp42"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "비밀번호에는 공백을 쓸 수 없습니다."));
    }

    @Test
    @DisplayName("로그인: 공백 비밀번호 시도는 실패 횟수에 넣지 않는다(계정·IP 차단 기준 5회를 넘겨도 정상 로그인 가능)")
    void whitespaceLoginDoesNotCountAsFailure() throws Exception {
        saveMember("nocount@example.com", "nocount", passwordEncoder.encode("right-password-9"));
        for (int i = 0; i < 6; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nocount@example.com\",\"password\":\"wrong pw " + i + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(apiLogin("nocount@example.com", "right-password-9")).isEqualTo("200");
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호에 공백이 있으면 해시 비교 전에 거절하고 바꾸지 않는다")
    void changeRejectsWhitespaceInCurrentPassword() throws Exception {
        saveMember("chg5@example.com", "chg5", passwordEncoder.encode("otter lamp 42"));
        mvc.perform(post("/member/edit").with(user("chg5@example.com").roles("MEMBER"))
                        .param("current_password", "otter lamp 42")
                        .param("new_password", "brand-new-phrase-7")
                        .param("new_password_confirm", "brand-new-phrase-7"))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attribute("pwError", "비밀번호에는 공백을 쓸 수 없습니다."));
        Member m = memberRepository.findByEmail("chg5@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("otter lamp 42", m.getPassword())).isTrue();
        assertThat(m.getTokenVersion()).isZero();
    }

    @Test
    @DisplayName("비밀번호 변경: 공백이 든 새 비밀번호는 거절하고 기존 비밀번호를 유지한다")
    void changeRejectsWhitespace() throws Exception {
        saveMember("chg4@example.com", "chg4", passwordEncoder.encode("old-password-123"));
        mvc.perform(post("/member/edit").with(user("chg4@example.com").roles("MEMBER"))
                        .param("current_password", "old-password-123")
                        .param("new_password", "brand new phrase 7")
                        .param("new_password_confirm", "brand new phrase 7"))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attribute("pwError", "비밀번호에는 공백을 쓸 수 없습니다."));
        Member m = memberRepository.findByEmail("chg4@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("old-password-123", m.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입: 특수문자가 든 비밀번호를 입력한 그대로 저장해 폼·API 로그인이 모두 된다")
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
    @DisplayName("비밀번호 변경: 현재 비밀번호가 틀리면 바뀌지 않는다")
    void changeRequiresCurrentPassword() throws Exception {
        saveMember("chg1@example.com", "chg1", passwordEncoder.encode("old-password-123"));

        mvc.perform(post("/member/edit").with(user("chg1@example.com").roles("MEMBER"))
                        .param("current_password", "not-my-password")
                        .param("new_password", "brand-new-phrase-7"))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attribute("pwError", "현재 비밀번호가 일치하지 않습니다."));

        Member m = memberRepository.findByEmail("chg1@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("old-password-123", m.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호 변경: 새 비밀번호도 정책을 지켜야 한다")
    void changeEnforcesPolicy() throws Exception {
        saveMember("chg2@example.com", "chg2", passwordEncoder.encode("old-password-123"));
        mvc.perform(post("/member/edit").with(user("chg2@example.com").roles("MEMBER"))
                        .param("current_password", "old-password-123")
                        .param("new_password", "qwerty123456"))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attributeExists("pwError"));
    }

    @Test
    @DisplayName("비밀번호 변경: 성공하면 기존 JWT 가 모두 무효가 되고 AT 쿠키를 지운다")
    void changeInvalidatesExistingTokens() throws Exception {
        saveMember("chg3@example.com", "chg3", passwordEncoder.encode("old-password-123"));

        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"chg3@example.com\",\"password\":\"old-password-123\"}")).andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        MvcResult change = mvc.perform(post("/member/edit").cookie(new Cookie("AT", token))
                        .param("current_password", "old-password-123")
                        .param("new_password", "brand-new-phrase-7")
                        .param("new_password_confirm", "brand-new-phrase-7"))
                .andExpect(redirectedUrl("/login?password=changed"))
                .andReturn();
        assertThat(change.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(c -> c.startsWith("AT=;") && c.contains("Max-Age=0"));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(apiLogin("chg3@example.com", "brand-new-phrase-7")).isEqualTo("200");
        assertThat(apiLogin("chg3@example.com", "old-password-123")).isEqualTo("401");
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
                        .content("{\"email\":\"made@example.com\",\"password\":\"long-enough-phrase-9\",\"nickname\":\"made\"}"))
                .andExpect(status().isCreated());
    }
}
