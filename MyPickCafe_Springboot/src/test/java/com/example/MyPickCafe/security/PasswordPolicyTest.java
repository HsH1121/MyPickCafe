package com.example.MyPickCafe.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.MyPickCafe.TestPasswords;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("대문자·숫자·특수문자 조합을 강제하지 않는다(띄어쓰기 없는 긴 문장 통과)")
    void noCompositionRules() {
        ok("bluetramatdawn");
        ok("오늘은홍대에서라떼한잔");
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
    @DisplayName("빈 값은 '입력해주세요', 공백만 입력하면 길이와 상관없이 공백 금지로 거절")
    void emptyAndBlank() {
        // 빈 문자열은 어떤 메시지에도 '포함'되므로 rejected() 의 원문 미포함 검사를 쓰지 않는다
        assertThatThrownBy(() -> policy.validate("", "someone@example.com", "nick"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("비밀번호를 입력해주세요.");
        assertThatThrownBy(() -> policy.validate(null, "someone@example.com", "nick"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("비밀번호를 입력해주세요.");
        rejected("              ", "공백을 쓸 수 없습니다"); // 14칸
        rejected("   ", "공백을 쓸 수 없습니다"); // 3칸 — 길이(8자) 메시지가 아니다
        rejected("\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000", "공백을 쓸 수 없습니다"); // 전각 공백
        rejected(" ".repeat(21), "공백을 쓸 수 없습니다"); // 21칸 — 길이(20자) 메시지가 아니다
        rejected("\t\t\t", "제어 문자"); // 탭은 제어 문자 메시지가 먼저
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "river otter lamp",       // 중간 스페이스
            " riverotterlamp",        // 앞 공백
            "riverotterlamp ",        // 뒤 공백
            "river\u00a0otterlamp",   // NBSP
            "river\u3000otterlamp",   // 전각 공백
            "river\u2003otterlamp",   // EM SPACE
            "오늘은 홍대에서 라떼"
    })
    @DisplayName("공백: 위치·종류와 상관없이 공백이 들어가면 거절(잘라내서 통과시키지 않음)")
    void whitespaceRejected(String pw) {
        rejected(pw, "공백을 쓸 수 없습니다");
    }

    @Test
    @DisplayName("containsWhitespace(로그인·현재 비밀번호 확인용): 공백 종류 전부 감지, 길이·흔함 등 다른 규칙은 보지 않음")
    void containsWhitespace() {
        // 스페이스, 탭, 줄바꿈, CR, NBSP, OGHAM, EN QUAD, HAIR SPACE, 좁은 NBSP, 수학 공백, 줄/문단 구분자, 전각 공백
        int[] spaces = {0x20, 0x09, 0x0A, 0x0D, 0xA0, 0x1680, 0x2000, 0x200A, 0x202F, 0x205F, 0x2028, 0x2029, 0x3000};
        for (int cp : spaces) {
            String pw = "abc" + new String(Character.toChars(cp)) + "def";
            org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace(pw))
                    .as("U+%04X 를 공백으로 봐야 함", cp).isTrue();
        }
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace(" leading")).isTrue();
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace("trailing ")).isTrue();

        // 공백이 없으면 false — 짧거나 흔한 비밀번호라도 로그인 검사에서는 막지 않는다
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace("1234")).isFalse();
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace("password")).isFalse();
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace("오늘은홍대에서라떼")).isFalse();
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace("☕🍰_-·")).isFalse();
        // 폭 없는 문자(U+200B ZERO WIDTH SPACE)는 유니코드상 공백(Zs)이 아니라 서식 문자(Cf)라 대상이 아니다
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace("ab" + (char) 0x200B + "cd")).isFalse();
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace("")).isFalse();
        org.assertj.core.api.Assertions.assertThat(PasswordPolicy.containsWhitespace(null)).isFalse();
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
    @DisplayName("키보드 줄을 넘겨 이어 친 20자(가장 짧은 줄 zxcvbnm, 중간 위치에서 시작)도 연속 문자열로 거절")
    void sequentialWrapsAtMaxLength() {
        rejected("nmzxcvbnmzxcvbnmzxcv", "흔하거나"); // 20자
        rejected(new StringBuilder("nmzxcvbnmzxcvbnmzxcv").reverse().toString(), "흔하거나");
        rejected("78901234567890123456", "흔하거나"); // 20자
    }

    @Test
    @DisplayName("이메일 아이디·닉네임이 들어간 비밀번호 거절")
    void personalInfo() {
        assertThatThrownBy(() -> policy.validate("johnsmith-coffee", "JohnSmith@example.com", "barista"))
                .hasMessageContaining("이메일 아이디나 닉네임");
        assertThatThrownBy(() -> policy.validate("my-barista-life-77", "a@example.com", "Barista"))
                .hasMessageContaining("이메일 아이디나 닉네임");
        // 3자 이하 아이디는 우연히 겹치기 쉬워 비교하지 않는다
        assertThatCode(() -> policy.validate("bob-is-a-builder-77", "bob@example.com", "xyz"))
                .doesNotThrowAnyException();
    }

    /* ===== TestPasswords 상수: 규칙마다 하나씩 ===== */

    /** (상수 이름, 값, 기대 메시지 일부). 통합 테스트(PasswordFlowIntegrationTest)도 같은 목록을 쓴다. */
    static Stream<Arguments> ruleViolations() {
        return Stream.of(
                Arguments.of("EMPTY", TestPasswords.EMPTY, "비밀번호를 입력해주세요."),
                Arguments.of("CONTROL_CHAR", TestPasswords.CONTROL_CHAR, "제어 문자"),
                Arguments.of("WHITESPACE", TestPasswords.WHITESPACE, PasswordPolicy.WHITESPACE_MESSAGE),
                Arguments.of("TOO_SHORT", TestPasswords.TOO_SHORT, "8자 이상"),
                Arguments.of("TOO_LONG", TestPasswords.TOO_LONG, "20자 이하"),
                Arguments.of("OVER_72_BYTES", TestPasswords.OVER_72_BYTES, "72바이트를 넘었습니다"),
                Arguments.of("BLOCKLISTED", TestPasswords.BLOCKLISTED, "흔하거나"),
                Arguments.of("REPETITIVE", TestPasswords.REPETITIVE, "흔하거나"),
                Arguments.of("SEQUENTIAL", TestPasswords.SEQUENTIAL, "흔하거나"),
                Arguments.of("COMMON_WORD_AFFIX", TestPasswords.COMMON_WORD_AFFIX, "흔하거나"),
                Arguments.of("CONTAINS_EMAIL_ID", TestPasswords.CONTAINS_EMAIL_ID, "이메일 아이디나 닉네임"),
                Arguments.of("CONTAINS_NICKNAME", TestPasswords.CONTAINS_NICKNAME, "이메일 아이디나 닉네임")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ruleViolations")
    @DisplayName("규칙별 위반 상수는 해당 규칙의 메시지로 거절되고, 메시지에 원문이 없다")
    void eachRuleRejectsItsConstant(String name, String pw, String messagePart) {
        assertThatThrownBy(() -> policy.validate(pw, TestPasswords.POLICY_EMAIL, TestPasswords.POLICY_NICKNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(messagePart)
                .satisfies(e -> {
                    if (!pw.isEmpty()) assertThat(e.getMessage()).doesNotContain(pw);
                });
    }

    @Test
    @DisplayName("'흔한 비밀번호' 메시지를 공유하는 네 상수는 각자 자기 규칙에만 걸린다")
    void commonRuleConstantsHitOnlyTheirOwnDetector() {
        String blocklisted = TestPasswords.BLOCKLISTED.toLowerCase();
        String repetitive = TestPasswords.REPETITIVE.toLowerCase();
        String sequential = TestPasswords.SEQUENTIAL.toLowerCase();
        String common = TestPasswords.COMMON_WORD_AFFIX.toLowerCase();

        // BLOCKLISTED: 반복·연속·흔한 단어 어디에도 안 걸리는데 거절된다 → 목록 때문이다
        assertThat(PasswordPolicy.isRepetitive(blocklisted)).isFalse();
        assertThat(PasswordPolicy.isSequential(blocklisted)).isFalse();
        assertThat(PasswordPolicy.isCommonWordWithAffixes(blocklisted)).isFalse();

        assertThat(PasswordPolicy.isRepetitive(repetitive)).isTrue();
        assertThat(PasswordPolicy.isSequential(repetitive)).isFalse();
        assertThat(PasswordPolicy.isCommonWordWithAffixes(repetitive)).isFalse();

        assertThat(PasswordPolicy.isRepetitive(sequential)).isFalse();
        assertThat(PasswordPolicy.isSequential(sequential)).isTrue();
        assertThat(PasswordPolicy.isCommonWordWithAffixes(sequential)).isFalse();

        assertThat(PasswordPolicy.isRepetitive(common)).isFalse();
        assertThat(PasswordPolicy.isSequential(common)).isFalse();
        assertThat(PasswordPolicy.isCommonWordWithAffixes(common)).isTrue();
    }

    @Test
    @DisplayName("이메일 아이디·닉네임 상수는 해당 회원 정보가 없으면 통과한다(그 규칙 하나만 어긴다)")
    void personalInfoConstantsOnlyBreakPersonalInfoRule() {
        assertThatCode(() -> policy.validate(TestPasswords.CONTAINS_EMAIL_ID, "someone@example.com", "nick"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validate(TestPasswords.CONTAINS_NICKNAME, "someone@example.com", "nick"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {TestPasswords.RAW, TestPasswords.VALID_NEW, TestPasswords.WRONG, TestPasswords.XSS})
    @DisplayName("정상 상수(RAW·VALID_NEW·WRONG·XSS)는 정책 검사 회원 정보로도 통과한다")
    void validConstantsPass(String pw) {
        assertThatCode(() -> policy.validate(pw, TestPasswords.POLICY_EMAIL, TestPasswords.POLICY_NICKNAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TestPasswords.HASH 는 RAW 의 해시다")
    void hashMatchesRaw() {
        assertThat(new BCryptPasswordEncoder().matches(TestPasswords.RAW, TestPasswords.HASH)).isTrue();
    }
}
