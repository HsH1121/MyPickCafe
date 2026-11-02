package com.example.MyPickCafe.security;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 새 비밀번호 정책. 비밀번호를 <b>설정</b>하는 모든 경로(회원가입, 비밀번호 변경, 관리자 회원 생성)가 이 클래스 하나로 검사한다.
 * 로그인할 때는 검사하지 않는다 — 정책 전에 가입한 회원도 기존 비밀번호로 계속 로그인할 수 있어야 한다.
 *
 * <p>NIST SP 800-63B 방향을 따른다. 대문자·숫자·특수문자 조합을 강제하지 않고, 길이와 흔한 비밀번호 차단을 우선한다.
 * <ul>
 *   <li>길이: 12자 이상 64자 이하. 글자 수는 유니코드 코드 포인트 기준(이모지·한글 1자 = 1자).</li>
 *   <li>BCrypt 는 72바이트까지만 쓴다(그 뒤는 잘리고, Spring Security 6.3+ 는 encode 에서 예외). 그래서 UTF-8 72바이트를 넘으면 거절한다.
 *       영문·숫자는 64자까지 되지만, 한글은 글자당 3바이트라 24자까지다.</li>
 *   <li>공백: 허용하며 앞뒤 공백도 잘라내지 않는다(입력한 그대로 해시). 다만 공백만으로 된 비밀번호는 거절한다.</li>
 *   <li>유니코드: 허용하며 정규화(NFC/NFKC)하지 않는다. 정규화를 새로 넣으면 기존 해시와 비교가 어긋날 수 있어서다.
 *       제어 문자(줄바꿈·탭 등)는 입력 방법에 따라 달라지므로 거절한다.</li>
 *   <li>흔한·유출 비밀번호: {@code security/common-passwords.txt} 목록, 같은 문자 반복, 연속 문자열(123456…, abcdef…, qwerty…),
 *       흔한 단어 + 숫자(password1234 등), 이메일 아이디·닉네임을 포함한 비밀번호를 거절한다.</li>
 * </ul>
 */
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 64;
    public static final int MAX_BCRYPT_BYTES = 72;

    /** 숫자·기호를 떼고 남았을 때 이 단어면 흔한 비밀번호로 본다(password1234!, qwerty123456 등). */
    private static final Set<String> COMMON_BASES = Set.of(
            "password", "passw0rd", "qwerty", "qwertyuiop", "asdfgh", "asdfghjkl", "zxcvbn", "zxcvbnm",
            "iloveyou", "admin", "administrator", "welcome", "letmein", "login", "dragon", "monkey",
            "sunshine", "princess", "football", "baseball", "master", "superman", "batman", "starwars",
            "trustno1", "abc", "abcd", "test", "guest", "user", "root", "secret", "changeme", "hello",
            "mypickcafe", "pickcafe", "cafe", "coffee", "love", "sarang", "saranghae", "korea", "seoul"
    );

    private static final String[] KEYBOARD_ROWS = {
            "1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm", "abcdefghijklmnopqrstuvwxyz"
    };

    private final Set<String> blocklist;

    public PasswordPolicy() {
        this.blocklist = loadBlocklist();
    }

    /**
     * 정책을 어기면 사용자에게 보여 줄 메시지와 함께 {@link IllegalArgumentException}.
     * 메시지에 비밀번호 원문은 넣지 않는다.
     *
     * @param email    비밀번호와 비교할 이메일(없으면 null)
     * @param nickname 비밀번호와 비교할 닉네임(없으면 null)
     */
    public void validate(String password, String email, String nickname) {
        if (password == null || password.isEmpty() || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 " + MIN_LENGTH + "자 이상이어야 합니다.");
        }
        if (length > MAX_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalArgumentException(
                    "비밀번호가 너무 깁니다. 영문·숫자는 64자, 한글은 24자까지 쓸 수 있습니다.");
        }
        if (password.codePoints().anyMatch(PasswordPolicy::isControl)) {
            throw new IllegalArgumentException("줄바꿈·탭 같은 제어 문자는 비밀번호에 쓸 수 없습니다.");
        }

        String lower = password.toLowerCase(Locale.ROOT);
        if (blocklist.contains(lower) || isRepetitive(lower) || isSequential(lower) || isCommonWordWithAffixes(lower)) {
            throw new IllegalArgumentException("너무 흔하거나 추측하기 쉬운 비밀번호입니다. 다른 비밀번호를 사용해주세요.");
        }
        if (containsPersonalInfo(lower, email, nickname)) {
            throw new IllegalArgumentException("이메일 아이디나 닉네임이 들어간 비밀번호는 사용할 수 없습니다.");
        }
    }

    private static boolean isControl(int cp) {
        int type = Character.getType(cp);
        return type == Character.CONTROL || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR;
    }

    /** 1~4글자 단위가 계속 반복되는 경우(aaaaaaaaaaaa, abababababab, abcabcabcabc). */
    static boolean isRepetitive(String s) {
        int[] cps = s.codePoints().toArray();
        for (int unit = 1; unit <= 4 && unit < cps.length; unit++) {
            boolean repeats = true;
            for (int i = unit; i < cps.length; i++) {
                if (cps[i] != cps[i - unit]) { repeats = false; break; }
            }
            if (repeats) return true;
        }
        return false;
    }

    /** 전체가 키보드 줄·알파벳·숫자 순서를 그대로(또는 거꾸로) 이어 친 문자열인 경우. */
    static boolean isSequential(String s) {
        String doubled;
        for (String row : KEYBOARD_ROWS) {
            doubled = row + row + row + row + row + row + row + row; // 1234567890 을 넘겨 이어 친 경우
            String reversed = new StringBuilder(doubled).reverse().toString();
            if (doubled.contains(s) || reversed.contains(s)) return true;
        }
        return false;
    }

    /** 앞뒤 숫자·기호를 떼면 흔한 단어만 남는 경우(password1234!, !!qwerty2024, 123456789abc 는 아님). */
    static boolean isCommonWordWithAffixes(String s) {
        String core = s.replaceAll("^[^\\p{L}]+", "").replaceAll("[^\\p{L}]+$", "");
        if (core.isEmpty()) return false; // 숫자·기호만 — 길이와 다른 규칙으로 판단
        return COMMON_BASES.contains(core);
    }

    private static boolean containsPersonalInfo(String lowerPassword, String email, String nickname) {
        if (email != null) {
            int at = email.indexOf('@');
            String local = (at > 0 ? email.substring(0, at) : email).toLowerCase(Locale.ROOT).trim();
            if (local.length() >= 4 && lowerPassword.contains(local)) return true;
        }
        if (nickname != null) {
            String nick = nickname.toLowerCase(Locale.ROOT).trim();
            if (nick.length() >= 4 && lowerPassword.contains(nick)) return true;
        }
        return false;
    }

    private static Set<String> loadBlocklist() {
        Set<String> set = new HashSet<>();
        ClassPathResource res = new ClassPathResource("security/common-passwords.txt");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                set.add(line.toLowerCase(Locale.ROOT));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("common-passwords.txt 를 읽지 못했습니다.", e);
        }
        return Set.copyOf(set);
    }
}
