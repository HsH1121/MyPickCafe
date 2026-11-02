package com.example.MyPickCafe.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt 에 "예전 폼 입력 호환"만 더한 PasswordEncoder.
 *
 * <p>예전에는 {@link XssSanitizingFilter} 가 비밀번호 폼 파라미터까지 Jsoup 으로 정리했다.
 * 그래서 폼으로 가입·변경한 비밀번호에 {@code & < >} 나 앞뒤·연속 공백이 있으면 원문이 아니라
 * <b>정리된 문자열</b>의 해시가 저장됐다. 이제 필터가 비밀번호를 건드리지 않으므로, 그 회원들이
 * 원문으로 로그인하면 해시가 맞지 않는다.
 *
 * <p>그래서 원문으로 맞지 않을 때, 원문을 예전 필터와 똑같이 정리한 값이 원문과 다를 때만 한 번 더 비교한다.
 * 예전 필터가 받아들이던 값과 같은 값만 통과하므로 새로 열리는 경로는 없다.
 * 새로 저장하는 해시는 항상 원문 기준이다({@link #encode}).
 */
public class LegacyFormPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegate;

    public LegacyFormPasswordEncoder() {
        this(new BCryptPasswordEncoder());
    }

    LegacyFormPasswordEncoder(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (delegate.matches(rawPassword, encodedPassword)) return true;
        if (rawPassword == null) return false;
        String raw = rawPassword.toString();
        String legacy = XssSanitizingFilter.sanitize(raw);
        return legacy != null && !legacy.equals(raw) && delegate.matches(legacy, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }
}
