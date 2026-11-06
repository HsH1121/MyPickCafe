package com.example.MyPickCafe;

/**
 * 테스트에서 쓰는 비밀번호를 한곳에 모은다. 같은 값을 여러 테스트에 문자열로 반복해 쓰지 않는다.
 *
 * <ul>
 *   <li><b>정상 값</b>: 정책(8~20자, 공백·제어 문자 없음, 흔한 비밀번호·이메일 아이디·닉네임 미포함)을 지킨다.</li>
 *   <li><b>XSS</b>: 정책은 지키면서 {@code & < > " '} 를 담아, XSS 필터가 비밀번호를 바꾸지 않는지 확인한다.</li>
 *   <li><b>규칙별 위반 값</b>: {@code PasswordPolicy.validate} 의 규칙 하나씩만 어기도록 만들었다.
 *       이메일 아이디·닉네임 규칙은 {@link #POLICY_EMAIL}·{@link #POLICY_NICKNAME} 과 함께 검사해야 걸린다.</li>
 * </ul>
 */
public final class TestPasswords {

    /* ===== 정상 값 ===== */

    /** 기본 비밀번호. 회원을 직접 저장할 때는 {@link #HASH} 를 쓴다. */
    public static final String RAW = "quiet-latte-8642";
    /** {@link #RAW} 의 BCrypt(cost 10) 해시. */
    public static final String HASH = "$2a$10$RddE4f4ICAt9JIFRGgIMY.DSloFanr4MtpX2Lr9AZRFCi68PUQDD.";
    /** 비밀번호 변경 테스트의 새 비밀번호. */
    public static final String VALID_NEW = "brand-new-phrase-7";
    /** 정책은 지키지만 저장된 비밀번호와 다른 값(틀린 비밀번호 입력용). */
    public static final String WRONG = "not-my-password";
    /** XSS 필터 확인용: & < > " ' 를 모두 담은 정상 값. 필터가 건드리면 원문과 다른 값이 해시된다. */
    public static final String XSS = "otter&<lamp>\"42'";

    /* ===== 규칙별 위반 값 — 검사 순서대로 ===== */

    /** 이메일 아이디·닉네임 규칙을 확인할 때 쓰는 회원 정보. 다른 위반 값에는 이 둘이 들어 있지 않다. */
    public static final String POLICY_EMAIL = "policy-check@example.com";
    public static final String POLICY_NICKNAME = "barista9";

    /** 빈 값 → "비밀번호를 입력해주세요." */
    public static final String EMPTY = "";
    /** 탭(U+0009) → 제어 문자 */
    public static final String CONTROL_CHAR = "otter" + (char) 0x09 + "lamp-42";
    /** 스페이스 → 공백 금지 */
    public static final String WHITESPACE = "otter lamp 42";
    /** 7자 → 8자 이상 */
    public static final String TOO_SHORT = "tz8#kvq";
    /** 21자 → 20자 이하 */
    public static final String TOO_LONG = "tz8#kvq2mwpLx7!r9@b3k";
    /** 4바이트 이모지 19개: 19자라 길이는 통과하지만 76바이트 → 72바이트 초과 */
    public static final String OVER_72_BYTES = emojis(19);
    /** security/common-passwords.txt 에만 있는 값(반복·연속·흔한 단어 규칙에는 안 걸림) → 흔한 비밀번호 */
    public static final String BLOCKLISTED = "qazwsxedc1";
    /** 2글자 반복 → 흔한 비밀번호(반복) */
    public static final String REPETITIVE = "k9k9k9k9k9";
    /** 알파벳 연속 → 흔한 비밀번호(연속) */
    public static final String SEQUENTIAL = "hijklmnopqrs";
    /** 흔한 단어 dragon + 앞뒤 기호·숫자 → 흔한 비밀번호(흔한 단어) */
    public static final String COMMON_WORD_AFFIX = "!!dragon2024";
    /** {@link #POLICY_EMAIL} 의 아이디(policy-check) 포함 → 이메일 아이디·닉네임 */
    public static final String CONTAINS_EMAIL_ID = "policy-check-77";
    /** {@link #POLICY_NICKNAME}(barista9) 포함 → 이메일 아이디·닉네임 */
    public static final String CONTAINS_NICKNAME = "my-barista9-life";

    /** U+1F400 부터 이어지는 서로 다른 이모지 n개(모두 UTF-8 4바이트, 반복 패턴 아님). */
    public static String emojis(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.appendCodePoint(0x1F400 + i);
        return sb.toString();
    }

    private TestPasswords() {}
}
