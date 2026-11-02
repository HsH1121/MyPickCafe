package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

/**
 * 이메일·비밀번호 확인과 로그인 시도 제한을 한곳에서 처리한다.
 * 폼 로그인({@code POST /login})과 REST 로그인({@code POST /api/auth/login})이 같은 규칙을 쓴다.
 * JWT 발급과 쿠키 설정은 기존처럼 각 컨트롤러가 한다.
 */
@Service
public class LoginService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService attempts;

    /**
     * 없는 이메일일 때도 BCrypt 비교를 한 번 해서 응답 시간을 맞춘다.
     * 이게 없으면 "없는 계정은 즉시, 있는 계정은 BCrypt 만큼 늦게" 응답해 가입 여부가 드러난다.
     */
    private final String dummyHash;

    public LoginService(MemberRepository memberRepository,
                        PasswordEncoder passwordEncoder,
                        LoginAttemptService attempts) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.attempts = attempts;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    public enum Status { SUCCESS, INVALID_CREDENTIALS, BLOCKED }

    /**
     * @param member     SUCCESS 일 때만 값이 있다
     * @param retryAfter BLOCKED 일 때만 값이 있다
     */
    public record Result(Status status, Member member, Duration retryAfter) {
        public boolean isSuccess() { return status == Status.SUCCESS; }
    }

    @Transactional(readOnly = true)
    public Result authenticate(String email, String rawPassword, String clientIp) {
        String ip = clientIp == null ? "" : clientIp;

        // 차단 중이면 비밀번호를 확인하지 않는다(차단 중에도 맞는 비밀번호를 알아낼 수 있으면 의미가 없다).
        Optional<Duration> blocked = attempts.blockedFor(email, ip);
        if (blocked.isPresent()) {
            return new Result(Status.BLOCKED, null, blocked.get());
        }

        Optional<Member> memberOpt = (email == null || email.isBlank())
                ? Optional.empty()
                : memberRepository.findByEmail(email);

        boolean matches;
        if (memberOpt.isEmpty()) {
            safeMatches(rawPassword, dummyHash);
            matches = false;
        } else {
            // 저장된 해시와만 비교한다(평문 폴백 없음).
            matches = safeMatches(rawPassword, memberOpt.get().getPassword());
        }

        if (!matches) {
            attempts.recordFailure(email, ip);
            return new Result(Status.INVALID_CREDENTIALS, null, null);
        }
        attempts.recordSuccess(email, ip);
        return new Result(Status.SUCCESS, memberOpt.get(), null);
    }

    private boolean safeMatches(String raw, String hash) {
        if (raw == null || hash == null) return false;
        try {
            return passwordEncoder.matches(raw, hash);
        } catch (IllegalArgumentException e) {
            // 해시 형식이 아닌 값이 저장된 경우 — 인증 실패로 처리
            return false;
        }
    }
}
