package com.example.MyPickCafe.security;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
 * 비밀번호를 설정·확인하는 경로를 끝에서 끝까지 확인한다. 비밀번호 값은 모두 {@link TestPasswords} 상수를 쓴다.
 *
 * <p>회원 생성은 일반회원(POST /signup, 폼 → AuthController)과 관리자(POST /api/members, JSON → MemberApiController)가
 * 서로 다른 엔드포인트라 각 테스트에서 둘 다 확인한다. 로그인과 비밀번호 변경(/member/edit)은 역할과 상관없이
 * 같은 엔드포인트·로직이라 일반회원으로만 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("비밀번호 설정 경로")
class PasswordFlowIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    /* ===== 도우미 ===== */

    private Member saveMember(String email, String nickname, String hash) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(hash);
        m.setNickname(nickname);
        m.setRoleKind(RoleKind.MEMBER);
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    /** 일반회원 가입(폼). */
    private MockHttpServletRequestBuilder signup(String email, String nickname, String pw) {
        return post("/signup")
                .param("email", email).param("password", pw).param("passwordConfirm", pw)
                .param("nickname", nickname)
                .param("age", "25").param("gender", "M");
    }

    /** 관리자 회원 생성(JSON). */
    private MockHttpServletRequestBuilder adminCreate(String email, String nickname, String pw) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", pw);
        body.put("nickname", nickname);
        return post("/api/members").with(user("admin@example.com").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private MockHttpServletRequestBuilder apiLoginRequest(String email, String pw) throws Exception {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", pw)));
    }

    private int apiLogin(String email, String pw) throws Exception {
        return mvc.perform(apiLoginRequest(email, pw)).andReturn().getResponse().getStatus();
    }

    /* ===== 회원 생성: 일반회원 + 관리자 ===== */

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> ruleViolations() {
        return PasswordPolicyTest.ruleViolations();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ruleViolations")
    @DisplayName("회원 생성: 규칙별 위반 비밀번호는 회원가입·관리자 생성 모두 해당 메시지로 거절되고 저장되지 않는다")
    void createRejectsEachRule(String name, String pw, String messagePart) throws Exception {
        // 일반회원: 폼 가입 → 같은 화면에 오류 메시지
        mvc.perform(signup(TestPasswords.POLICY_EMAIL, TestPasswords.POLICY_NICKNAME, pw))
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attribute("error", containsString(messagePart)));
        assertThat(memberRepository.findByEmail(TestPasswords.POLICY_EMAIL)).as("회원가입 [%s]", name).isEmpty();

        // 관리자: JSON 생성 → 400. 빈 값은 DTO 의 @NotBlank 가 먼저 막아 필드 오류로 응답한다.
        var admin = mvc.perform(adminCreate(TestPasswords.POLICY_EMAIL, TestPasswords.POLICY_NICKNAME, pw))
                .andExpect(status().isBadRequest());
        if (pw.isEmpty()) {
            admin.andExpect(jsonPath("$.errors.password").exists());
        } else {
            admin.andExpect(jsonPath("$.message", containsString(messagePart)));
        }
        assertThat(memberRepository.findByEmail(TestPasswords.POLICY_EMAIL)).as("관리자 생성 [%s]", name).isEmpty();
    }

    @Test
    @DisplayName("회원 생성(XSS): & < > \" ' 가 든 비밀번호를 회원가입·관리자 생성 모두 원문 그대로 해시하고, 그 값으로 로그인된다")
    void createStoresRawPasswordWithoutXssFiltering() throws Exception {
        // 일반회원: 폼 파라미터라 XSS 필터를 지난다 — 비밀번호 파라미터는 정리하지 않아야 한다
        mvc.perform(signup("xss-member@example.com", "xss-member", TestPasswords.XSS))
                .andExpect(redirectedUrl("/login"));
        Member member = memberRepository.findByEmail("xss-member@example.com").orElseThrow();
        assertThat(passwordEncoder.matches(TestPasswords.XSS, member.getPassword()))
                .as("회원가입: XSS 필터가 & < > \" ' 를 바꾸지 않고 원문을 해시해야 한다").isTrue();

        // 관리자: JSON 본문
        mvc.perform(adminCreate("xss-admin@example.com", "xss-admin", TestPasswords.XSS))
                .andExpect(status().isCreated());
        Member made = memberRepository.findByEmail("xss-admin@example.com").orElseThrow();
        assertThat(passwordEncoder.matches(TestPasswords.XSS, made.getPassword()))
                .as("관리자 생성: 원문을 해시해야 한다").isTrue();

        // 두 계정 모두 같은 원문으로 폼·API 로그인
        for (String email : new String[]{"xss-member@example.com", "xss-admin@example.com"}) {
            assertThat(apiLogin(email, TestPasswords.XSS)).as("API 로그인 %s", email).isEqualTo(200);
            mvc.perform(post("/login").param("memberEmail", email).param("memberPassword", TestPasswords.XSS))
                    .andExpect(redirectedUrl("/"));
        }
    }

    /* ===== 로그인: 역할과 무관하게 같은 엔드포인트 → 일반회원만 ===== */

    @Test
    @DisplayName("API 로그인: 공백이 든 비밀번호는 해시 비교 전에 400 으로 거절한다(저장된 해시와 같아도)")
    void apiLoginRejectsWhitespace() throws Exception {
        // 정책상 생길 수 없는 값이지만, 해시 비교 전에 막는지 보려고 공백 비밀번호 해시를 직접 넣어 둔다.
        saveMember("spaced@example.com", "spaced", passwordEncoder.encode(TestPasswords.WHITESPACE));

        mvc.perform(apiLoginRequest("spaced@example.com", TestPasswords.WHITESPACE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordPolicy.WHITESPACE_MESSAGE));
        mvc.perform(apiLoginRequest("spaced@example.com", TestPasswords.RAW + (char) 0x3000)) // 끝에 전각 공백
                .andExpect(status().isBadRequest());
        // 없는 이메일도 같은 응답 — 가입 여부가 드러나지 않는다
        mvc.perform(apiLoginRequest("nobody@example.com", TestPasswords.WHITESPACE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordPolicy.WHITESPACE_MESSAGE));
    }

    @Test
    @DisplayName("API 로그인: 빈 비밀번호는 '입력해주세요', 공백만 보낸 비밀번호는 공백 금지로 400")
    void apiLoginEmptyVsBlankPassword() throws Exception {
        mvc.perform(apiLoginRequest("someone@example.com", TestPasswords.EMPTY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일과 비밀번호를 입력해주세요."));
        mvc.perform(apiLoginRequest("someone@example.com", "    "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordPolicy.WHITESPACE_MESSAGE));
    }

    @Test
    @DisplayName("폼 로그인: 공백이 든 비밀번호는 거절하고 이메일만 다시 채운다")
    void formLoginRejectsWhitespace() throws Exception {
        saveMember("spaced2@example.com", "spaced2", passwordEncoder.encode(TestPasswords.WHITESPACE));
        mvc.perform(post("/login").param("memberEmail", "spaced2@example.com").param("memberPassword", TestPasswords.WHITESPACE))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", PasswordPolicy.WHITESPACE_MESSAGE))
                .andExpect(flash().attribute("prefillEmail", "spaced2@example.com"));
        mvc.perform(post("/login").param("memberEmail", "spaced2@example.com").param("memberPassword", " " + TestPasswords.RAW))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", PasswordPolicy.WHITESPACE_MESSAGE));
    }

    @Test
    @DisplayName("로그인: 공백 비밀번호 시도는 실패 횟수에 넣지 않는다(계정·IP 차단 기준 5회를 넘겨도 정상 로그인 가능)")
    void whitespaceLoginDoesNotCountAsFailure() throws Exception {
        saveMember("nocount@example.com", "nocount", TestPasswords.HASH);
        for (int i = 0; i < 6; i++) {
            mvc.perform(apiLoginRequest("nocount@example.com", TestPasswords.WHITESPACE + i))
                    .andExpect(status().isBadRequest());
        }
        assertThat(apiLogin("nocount@example.com", TestPasswords.RAW)).isEqualTo(200);
    }

    /* ===== 비밀번호 변경: 역할과 무관하게 /member/edit → 일반회원만 ===== */

    @ParameterizedTest(name = "{0}")
    @MethodSource("ruleViolations")
    @DisplayName("비밀번호 변경: 규칙별 위반 새 비밀번호는 해당 메시지로 거절하고 기존 비밀번호를 유지한다")
    void changeRejectsEachRule(String name, String pw, String messagePart) throws Exception {
        // 새 비밀번호가 비어 있으면 '변경 안 함'이라 정책 대상이 아니다 — 통과가 아니라 건너뜀으로 남긴다
        org.junit.jupiter.api.Assumptions.assumeFalse(pw.isEmpty(), "빈 새 비밀번호는 변경 요청이 아님");
        saveMember(TestPasswords.POLICY_EMAIL, TestPasswords.POLICY_NICKNAME, TestPasswords.HASH);

        mvc.perform(post("/member/edit").with(user(TestPasswords.POLICY_EMAIL).roles("MEMBER"))
                        .param("current_password", TestPasswords.RAW)
                        .param("new_password", pw)
                        .param("new_password_confirm", pw))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attribute("pwError", containsString(messagePart)));

        Member m = memberRepository.findByEmail(TestPasswords.POLICY_EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches(TestPasswords.RAW, m.getPassword())).as("[%s] 기존 비밀번호 유지", name).isTrue();
        assertThat(m.getTokenVersion()).isZero();
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호에 공백이 있으면 해시 비교 전에 거절하고 바꾸지 않는다")
    void changeRejectsWhitespaceInCurrentPassword() throws Exception {
        saveMember("chg5@example.com", "chg5", passwordEncoder.encode(TestPasswords.WHITESPACE));
        mvc.perform(post("/member/edit").with(user("chg5@example.com").roles("MEMBER"))
                        .param("current_password", TestPasswords.WHITESPACE)
                        .param("new_password", TestPasswords.VALID_NEW)
                        .param("new_password_confirm", TestPasswords.VALID_NEW))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attribute("pwError", PasswordPolicy.WHITESPACE_MESSAGE));
        Member m = memberRepository.findByEmail("chg5@example.com").orElseThrow();
        assertThat(passwordEncoder.matches(TestPasswords.WHITESPACE, m.getPassword())).isTrue();
        assertThat(m.getTokenVersion()).isZero();
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호가 틀리면 바뀌지 않는다")
    void changeRequiresCurrentPassword() throws Exception {
        saveMember("chg1@example.com", "chg1", TestPasswords.HASH);

        mvc.perform(post("/member/edit").with(user("chg1@example.com").roles("MEMBER"))
                        .param("current_password", TestPasswords.WRONG)
                        .param("new_password", TestPasswords.VALID_NEW))
                .andExpect(redirectedUrl("/member/edit"))
                .andExpect(flash().attribute("pwError", "현재 비밀번호가 일치하지 않습니다."));

        Member m = memberRepository.findByEmail("chg1@example.com").orElseThrow();
        assertThat(passwordEncoder.matches(TestPasswords.RAW, m.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호 변경: 성공하면 기존 JWT 가 모두 무효가 되고 AT 쿠키를 지운다")
    void changeInvalidatesExistingTokens() throws Exception {
        saveMember("chg3@example.com", "chg3", TestPasswords.HASH);

        MvcResult login = mvc.perform(apiLoginRequest("chg3@example.com", TestPasswords.RAW)).andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        MvcResult change = mvc.perform(post("/member/edit").cookie(new Cookie("AT", token))
                        .param("current_password", TestPasswords.RAW)
                        .param("new_password", TestPasswords.VALID_NEW)
                        .param("new_password_confirm", TestPasswords.VALID_NEW))
                .andExpect(redirectedUrl("/login?password=changed"))
                .andReturn();
        assertThat(change.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(c -> c.startsWith("AT=;") && c.contains("Max-Age=0"));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(apiLogin("chg3@example.com", TestPasswords.VALID_NEW)).isEqualTo(200);
        assertThat(apiLogin("chg3@example.com", TestPasswords.RAW)).isEqualTo(401);
    }
}
