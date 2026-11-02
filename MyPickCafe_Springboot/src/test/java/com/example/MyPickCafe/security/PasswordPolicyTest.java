package com.example.MyPickCafe.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("비밀번호 정책")
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    private void ok(String pw) {
        assertThatCode(() -> policy.validate(pw, "someone@example.com", "nick"))
                .as("통과해야 함: [%s]", pw).doesNotThrowAnyException();
    }

    private void rejected(String pw, String messagePart) {
        assertThatThrownBy(() -> policy.validate(pw, "someone@example.com", "nick"))
                .as("거절해야 함: [%s]", pw)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(messagePart)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(e.getMessage()).doesNotContain(pw));
    }

    @Test
    @DisplayName("길이 경계")
    void lengthBoundaries() {
        rejected("tz8#kvq2mwp", "12자 이상");
        ok("tz8#kvq2mwpL");
        ok("tz8#kvq2mwpL".repeat(5) + "abcd"); // 64자
        rejected("tz8#kvq2mwpL".repeat(5) + "abcde", "64자 이하"); // 65자
    }

    @Test
    @DisplayName("대문자·숫자·특수문자 조합을 강제하지 않는다(긴 문장 통과)")
    void noCompositionRules() {
        ok("blue tram under the bridge");
        ok("오늘은 홍대에서 라떼 한잔");
    }

    @Test
    @DisplayName("글자 수는 코드 포인트 기준 — 이모지 12개는 12자")
    void countsCodePoints() {
        ok("☕🍰🍩🧁🍪🍫🍬🍭🍮🍯🥐🥯"); // 12 code points(UTF-16 으로는 23칸), 3 + 4×11 = 47바이트
    }

    @Test
    @DisplayName("BCrypt 72바이트 한도: 한글 24자 통과, 25자 거절")
    void bcryptByteLimit() {
        ok("가나다라마바사아자차카타파하거너더러머버서어저처"); // 24자 = 72바이트
        rejected("가나다라마바사아자차카타파하거너더러머버서어저처커", "너무 깁니다"); // 25자 = 75바이트
    }

    @Test
    @DisplayName("정책을 통과한 값은 BCrypt encode 에서 예외가 나지 않는다")
    void passingPasswordsAreEncodable() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
        assertThatCode(() -> bcrypt.encode("가나다라마바사아자차카타파하거너더러머버서어저처")).doesNotThrowAnyException();
        assertThatCode(() -> bcrypt.encode("tz8#kvq2mwpL".repeat(5) + "abcd")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공백: 앞뒤 공백 포함 허용(잘라내지 않음), 공백만은 거절")
    void whitespace() {
        ok("  river otter lamp  ");
        rejected("              ", "입력해주세요");
    }

    @Test
    @DisplayName("제어 문자(줄바꿈·탭) 거절")
    void controlCharacters() {
        rejected("river otter\nlamp", "제어 문자");
        rejected("river otter\tlamp", "제어 문자");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1q2w3e4r5t6y", "qazwsxedcrfv", "dkssudgktpdy", "Password1234", "P@ssw0rd1234",
            "aaaaaaaaaaaa", "abcabcabcabc", "123456789012", "abcdefghijklm", "zyxwvutsrqpo",
            "password!!2024", "qwerty123456", "iloveyou2024!", "mypickcafe2024"
    })
    @DisplayName("흔한·유출·패턴 비밀번호 거절")
    void commonPasswords(String pw) {
        rejected(pw, "흔하거나");
    }

    @Test
    @DisplayName("이메일 아이디·닉네임이 들어간 비밀번호 거절")
    void personalInfo() {
        assertThatThrownBy(() -> policy.validate("johnsmith-loves-coffee", "JohnSmith@example.com", "barista"))
                .hasMessageContaining("이메일 아이디나 닉네임");
        assertThatThrownBy(() -> policy.validate("my barista life 77", "a@example.com", "Barista"))
                .hasMessageContaining("이메일 아이디나 닉네임");
        // 3자 이하 아이디는 우연히 겹치기 쉬워 비교하지 않는다
        assertThatCode(() -> policy.validate("bob is a builder 77", "bob@example.com", "xyz"))
                .doesNotThrowAnyException();
    }
}
