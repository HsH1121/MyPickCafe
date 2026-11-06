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

    /** U+1F400 부터 이어지는 서로 다른 이모지 n개(모두 UTF-8 4바이트, 반복 패턴 아님). */
    private static String emojis(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.appendCodePoint(0x1F400 + i);
        return sb.toString();
    }

    @Test
    @DisplayName("길이 경계: 8자 이상 20자 이하")
    void lengthBoundaries() {
        rejected("tz8#kvq", "8자 이상"); // 7자
        ok("tz8#kvq2"); // 8자
        ok("tz8#kvq2mwpLx7!r9@b3"); // 20자
        rejected("tz8#kvq2mwpLx7!r9@b3k", "20자 이하"); // 21자
    }

    @Test
    @DisplayName("대문자·숫자·특수문자 조합을 강제하지 않는다(긴 문장 통과)")
    void noCompositionRules() {
        ok("blue tram at dawn");
        ok("오늘은 홍대에서 라떼 한잔");
    }

    @Test
    @DisplayName("글자 수는 코드 포인트 기준 — 이모지 12개는 12자")
    void countsCodePoints() {
        ok("☕🍰🍩🧁🍪🍫🍬🍭🍮🍯🥐🥯"); // 12 code points(UTF-16 으로는 23칸), 3 + 4×11 = 47바이트
    }

    @Test
    @DisplayName("BCrypt 72바이트 한도: 한글 20자(60바이트) 통과, 이모지 18자(72바이트) 통과·19자(76바이트) 거절")
    void bcryptByteLimit() {
        ok("가나다라마바사아자차카타파하거너더러머버"); // 20자 = 60바이트
        ok(emojis(18)); // 18자 = 72바이트
        rejected(emojis(19), "비밀번호가 72바이트를 넘었습니다."); // 19자 = 76바이트
        rejected(emojis(17) + "가나다", "비밀번호가 72바이트를 넘었습니다."); // 20자지만 68 + 9 = 77바이트
    }

    @Test
    @DisplayName("정책을 통과한 값은 BCrypt encode 에서 예외가 나지 않는다")
    void passingPasswordsAreEncodable() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
        assertThatCode(() -> bcrypt.encode("가나다라마바사아자차카타파하거너더러머버")).doesNotThrowAnyException();
        assertThatCode(() -> bcrypt.encode(emojis(18))).doesNotThrowAnyException();
        assertThatCode(() -> bcrypt.encode("tz8#kvq2mwpLx7!r9@b3")).doesNotThrowAnyException();
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
            "password!!2024", "qwerty123456", "iloveyou2024!", "mypickcafe2024",
            // 8~11자 — 최소 길이를 8자로 내리면서 새로 막아야 하는 값
            "1q2w3e4r", "q1w2e3r4", "qwer1234", "asdf1234", "1qaz2wsx", "zaq12wsx", "1234qwer",
            "a1b2c3d4", "11223344", "tkfkdgo1", "trustno1", "password", "12345678", "iloveyou",
            "11111111", "abcd1234", "qwerty12", "shadow123", "computer"
    })
    @DisplayName("흔한·유출·패턴 비밀번호 거절")
    void commonPasswords(String pw) {
        rejected(pw, "흔하거나");
    }

    @Test
    @DisplayName("이메일 아이디·닉네임이 들어간 비밀번호 거절")
    void personalInfo() {
        assertThatThrownBy(() -> policy.validate("johnsmith-coffee", "JohnSmith@example.com", "barista"))
                .hasMessageContaining("이메일 아이디나 닉네임");
        assertThatThrownBy(() -> policy.validate("my barista life 77", "a@example.com", "Barista"))
                .hasMessageContaining("이메일 아이디나 닉네임");
        // 3자 이하 아이디는 우연히 겹치기 쉬워 비교하지 않는다
        assertThatCode(() -> policy.validate("bob is a builder 77", "bob@example.com", "xyz"))
                .doesNotThrowAnyException();
    }
}
