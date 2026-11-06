package com.example.MyPickCafe.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 입력 화면의 안내(password-policy.js, 회원가입·회원정보 템플릿)가 서버 {@link PasswordPolicy} 의 길이 기준과 같은지 본다.
 * 서버 숫자만 바꾸고 화면을 놓치면 "화면은 통과, 서버는 거절"이 되므로 여기서 걸리게 한다.
 */
@DisplayName("비밀번호 정책: 화면 안내가 서버 기준과 같다")
class PasswordPolicyFrontendSyncTest {

    private static String read(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("password-policy.js 의 MIN·MAX·MAX_BYTES")
    void scriptConstants() throws IOException {
        Matcher m = Pattern.compile("var MIN = (\\d+), MAX = (\\d+), MAX_BYTES = (\\d+);")
                .matcher(read("static/js/password-policy.js"));
        assertThat(m.find()).as("password-policy.js 에서 상수 선언을 찾지 못함").isTrue();
        assertThat(Integer.parseInt(m.group(1))).isEqualTo(PasswordPolicy.MIN_LENGTH);
        assertThat(Integer.parseInt(m.group(2))).isEqualTo(PasswordPolicy.MAX_LENGTH);
        assertThat(Integer.parseInt(m.group(3))).isEqualTo(PasswordPolicy.MAX_BCRYPT_BYTES);
    }

    @ParameterizedTest
    @ValueSource(strings = {"templates/auth/signup.mustache", "templates/member/edit.mustache"})
    @DisplayName("템플릿 안내 문구의 글자 수 범위, 그리고 UTF-16 기준인 maxlength 를 쓰지 않는지")
    void templateHints(String path) throws IOException {
        String html = read(path);
        assertThat(html).contains(PasswordPolicy.MIN_LENGTH + "~" + PasswordPolicy.MAX_LENGTH + "자");
        // 브라우저 maxlength 는 이모지를 2칸으로 세서, 서버가 받아 주는 이모지 비밀번호를 입력 단계에서 잘라 버린다.
        Matcher pw = Pattern.compile("<input[^>]*type=\"password\"[^>]*>").matcher(html);
        while (pw.find()) {
            assertThat(pw.group()).as("비밀번호 입력에 maxlength: %s", pw.group()).doesNotContain("maxlength");
        }
    }
}
