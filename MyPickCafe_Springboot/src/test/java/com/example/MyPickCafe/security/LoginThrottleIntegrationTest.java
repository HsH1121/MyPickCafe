package com.example.MyPickCafe.security;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인 API·폼 로그인에 실패 횟수 제한이 걸리는지, 응답이 가입 여부를 드러내지 않는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("로그인 무차별 대입 방어")
class LoginThrottleIntegrationTest {

    private static final String EMAIL = "throttle@example.com";
    private static final String PASSWORD = TestPasswords.RAW;

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LoginAttemptService attempts;

    @BeforeEach
    void setUp() {
        attempts.clear();
        Member m = new Member();
        m.setEmail(EMAIL);
        m.setPassword(passwordEncoder.encode(PASSWORD));
        m.setNickname("throttle");
        m.setRoleKind(RoleKind.MEMBER);
        m.setTokenVersion(0L);
        memberRepository.save(m);
    }

    private MockHttpServletRequestBuilder apiLogin(String email, String password, String ip) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .with(req -> { req.setRemoteAddr(ip); return req; });
    }

    private MockHttpServletRequestBuilder formLogin(String email, String password, String ip) {
        return post("/login")
                .param("memberEmail", email)
                .param("memberPassword", password)
                .with(req -> { req.setRemoteAddr(ip); return req; });
    }

    @Test
    @DisplayName("API: 5번 틀리면 맞는 비밀번호도 429, 다른 IP 는 정상 로그인")
    void apiBlocksAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(apiLogin(EMAIL, "wrong-" + i, "203.0.113.10")).andExpect(status().isUnauthorized());
        }
        mvc.perform(apiLogin(EMAIL, PASSWORD, "203.0.113.10"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        mvc.perform(apiLogin(EMAIL, PASSWORD, "198.51.100.20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API: 없는 이메일과 틀린 비밀번호의 응답이 같다")
    void apiDoesNotRevealAccountExistence() throws Exception {
        String unknown = mvc.perform(apiLogin("nobody@example.com", TestPasswords.WRONG, "203.0.113.11"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String wrong = mvc.perform(apiLogin(EMAIL, TestPasswords.WRONG, "203.0.113.11"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(unknown).isEqualTo(wrong);
    }

    @Test
    @DisplayName("API: 없는 이메일도 같은 규칙으로 막혀 차단 응답으로 가입 여부를 알 수 없다")
    void unknownAccountIsThrottledToo() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(apiLogin("nobody@example.com", "x" + i, "203.0.113.12"));
        }
        mvc.perform(apiLogin("nobody@example.com", "x", "203.0.113.12"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("폼: 5번 틀리면 차단 안내, 맞는 비밀번호로도 쿠키가 발급되지 않는다")
    void formBlocksAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(formLogin(EMAIL, "wrong-" + i, "203.0.113.13"))
                    .andExpect(flash().attribute("error", "이메일 또는 비밀번호가 올바르지 않습니다."));
        }
        MvcResult r = mvc.perform(formLogin(EMAIL, PASSWORD, "203.0.113.13")).andReturn();
        assertThat(r.getResponse().getRedirectedUrl()).isEqualTo("/login");
        assertThat((String) r.getFlashMap().get("error")).startsWith("로그인 시도가 너무 많습니다.");
        assertThat(r.getResponse().getHeaders("Set-Cookie")).noneMatch(c -> c.startsWith("AT="));
    }

    @Test
    @DisplayName("폼: 실패해도 비밀번호를 플래시(세션)에 담지 않고, 로그인 화면에 다시 쓰지 않는다")
    void formDoesNotEchoPassword() throws Exception {
        String secret = "my-secret-guess-xyz";
        MvcResult r = mvc.perform(formLogin(EMAIL, secret, "203.0.113.14")).andReturn();
        java.util.Map<String, Object> flash = r.getFlashMap();
        assertThat(flash).doesNotContainKey("prefillPassword");
        assertThat(r.getFlashMap().values()).noneMatch(v -> secret.equals(v));

        mvc.perform(get("/login").flashAttrs(r.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(secret))));
    }

    @Test
    @DisplayName("X-Forwarded-For 를 바꿔 보내도 다른 IP 로 취급되지 않는다")
    void spoofedForwardedForIsIgnored() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(apiLogin(EMAIL, "wrong-" + i, "203.0.113.15")
                    .header("X-Forwarded-For", "10.9.8." + i));
        }
        mvc.perform(apiLogin(EMAIL, PASSWORD, "203.0.113.15").header("X-Forwarded-For", "10.9.8.99"))
                .andExpect(status().isTooManyRequests());
    }
}
