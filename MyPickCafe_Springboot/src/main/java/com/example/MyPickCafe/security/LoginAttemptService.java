package com.example.MyPickCafe.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 로그인 실패 횟수 제한(무차별 대입·크리덴셜 스터핑 방어).
 *
 * <p>세 가지 키로 실패를 센다. 정해진 시간(window) 안에 한도를 넘기면 일정 시간(block) 동안
 * 비밀번호를 확인하지 않고 거절한다. 실패 횟수만 세며 성공한 로그인은 세지 않는다.
 * <ol>
 *   <li><b>계정+IP</b> (기본 5분 동안 5회 → 5분 차단): 한 곳에서 한 계정의 비밀번호를 추측하는 경우.
 *       다른 IP 에서 쓰는 실제 사용자는 영향을 받지 않는다.</li>
 *   <li><b>계정 전체</b> (기본 15분 동안 20회 → 15분 차단): 여러 IP 로 나눠 한 계정을 노리는 경우.
 *       제삼자가 이 한도를 일부러 채워 실제 사용자를 막을 수 있으므로, 최근에 그 계정으로
 *       로그인에 성공한 적 있는 IP 는 이 차단을 받지 않는다(1번 한도는 그대로 적용).</li>
 *   <li><b>IP 전체</b> (기본 15분 동안 50회 → 15분 차단): 한 IP 에서 여러 계정을 돌아가며 시도하는 경우.
 *       회사·학교·통신사 NAT 처럼 여러 사람이 한 IP 를 쓰는 경우를 생각해 한도를 높게 잡는다.</li>
 * </ol>
 *
 * <p>IP 는 {@code HttpServletRequest#getRemoteAddr()} 를 쓴다. X-Forwarded-For 를 직접 읽지 않는다.
 * prod 에서는 Tomcat 이 {@code server.forward-headers-strategy=native} 로 사설망 프록시(caddy)가 넣은
 * 값만 반영하고, caddy 는 클라이언트가 보낸 X-Forwarded-For 를 버린다.
 *
 * <p><b>한계</b>: 상태를 이 JVM 의 메모리에 둔다. 서버를 재시작하면 기록이 사라지고,
 * 인스턴스를 여러 대로 늘리면 인스턴스마다 따로 센다(한도가 사실상 인스턴스 수만큼 늘어남).
 * 지금 배포(웹 컨테이너 1대)에는 맞지만, 다중 인스턴스로 가면 Redis 같은 공유 저장소로 옮겨야 한다.
 */
@Slf4j
@Component
public class LoginAttemptService {

    /** 추적하는 키 수 상한. 무작위 이메일을 쏟아부어 메모리를 채우는 공격을 막는다. */
    private static final int MAX_TRACKED_KEYS = 100_000;
    /** 이 횟수마다 한 번씩 만료된 기록을 치운다. */
    private static final int SWEEP_EVERY = 1_000;

    private final Clock clock;
    private final Limit accountIpLimit;
    private final Limit accountLimit;
    private final Limit ipLimit;
    private final Duration trustedIpTtl;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    /** "계정|IP" → 마지막 로그인 성공 시각. 계정 전체 차단을 받지 않는 IP. */
    private final Map<String, Instant> trustedPairs = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();

    @Autowired
    public LoginAttemptService(
            @Value("${app.login-throttle.account-ip.max-failures:5}") int accountIpMax,
            @Value("${app.login-throttle.account-ip.window:PT5M}") Duration accountIpWindow,
            @Value("${app.login-throttle.account-ip.block:PT5M}") Duration accountIpBlock,
            @Value("${app.login-throttle.account.max-failures:20}") int accountMax,
            @Value("${app.login-throttle.account.window:PT15M}") Duration accountWindow,
            @Value("${app.login-throttle.account.block:PT15M}") Duration accountBlock,
            @Value("${app.login-throttle.ip.max-failures:50}") int ipMax,
            @Value("${app.login-throttle.ip.window:PT15M}") Duration ipWindow,
            @Value("${app.login-throttle.ip.block:PT15M}") Duration ipBlock,
            @Value("${app.login-throttle.trusted-ip-ttl:P30D}") Duration trustedIpTtl) {
        this(Clock.systemUTC(),
                new Limit(accountIpMax, accountIpWindow, accountIpBlock),
                new Limit(accountMax, accountWindow, accountBlock),
                new Limit(ipMax, ipWindow, ipBlock),
                trustedIpTtl);
    }

    LoginAttemptService(Clock clock, Limit accountIp, Limit account, Limit ip, Duration trustedIpTtl) {
        this.clock = clock;
        this.accountIpLimit = accountIp;
        this.accountLimit = account;
        this.ipLimit = ip;
        this.trustedIpTtl = trustedIpTtl;
    }

    /**
     * 지금 로그인 시도를 막아야 하면 남은 차단 시간을 돌려준다.
     * 계정이 실제로 있는지와 관계없이 같은 규칙을 적용해, 차단 응답으로 가입 여부를 알 수 없게 한다.
     */
    public Optional<Duration> blockedFor(String email, String ip) {
        Instant now = clock.instant();
        String account = normalize(email);
        Duration longest = max(remaining(ipKey(ip), now), remaining(accountIpKey(account, ip), now));
        if (!isTrusted(account, ip, now)) {
            longest = max(longest, remaining(accountKey(account), now));
        }
        return longest.isZero() ? Optional.empty() : Optional.of(longest);
    }

    /** 비밀번호가 틀렸거나 없는 계정이면 호출한다. */
    public void recordFailure(String email, String ip) {
        Instant now = clock.instant();
        String account = normalize(email);
        boolean blocked = false;
        blocked |= hit(accountIpKey(account, ip), accountIpLimit, now);
        blocked |= hit(accountKey(account), accountLimit, now);
        blocked |= hit(ipKey(ip), ipLimit, now);
        if (blocked) {
            // 비밀번호는 남기지 않는다. 이메일은 앞 두 글자만 남긴다.
            log.warn("로그인 실패 한도 초과로 일시 차단: account={} ip={}", mask(account), ip);
        }
        maybeSweep(now);
    }

    /**
     * 로그인 성공. 이 계정+IP 의 실패 기록을 지우고, 이 IP 를 계정 전체 차단의 예외로 기억한다.
     * 계정 전체·IP 전체 기록은 지우지 않는다. 지우면 분산 공격 중에 실제 사용자가 한 번 로그인할 때마다
     * 공격자에게 시도 횟수가 다시 주어진다.
     */
    public void recordSuccess(String email, String ip) {
        Instant now = clock.instant();
        String account = normalize(email);
        counters.remove(accountIpKey(account, ip));
        trustedPairs.put(account + "|" + ip, now);
        maybeSweep(now);
    }

    /** 테스트용 — 모든 기록을 지운다. */
    void clear() {
        counters.clear();
        trustedPairs.clear();
    }

    // ── 내부 ─────────────────────────────────────────────────────────────

    record Limit(int maxFailures, Duration window, Duration block) {}

    /** 고정 창(window) 카운터. 첫 실패부터 window 안에 maxFailures 번 실패하면 blockedUntil 까지 차단. */
    private static final class Counter {
        final Instant windowStart;
        final int failures;
        final Instant blockedUntil;

        Counter(Instant windowStart, int failures, Instant blockedUntil) {
            this.windowStart = windowStart;
            this.failures = failures;
            this.blockedUntil = blockedUntil;
        }

        /** 기존의 실패 기록 만료 체킹 */
        boolean expired(Instant now, Limit limit) {
            boolean windowOver = !now.isBefore(windowStart.plus(limit.window()));
            boolean blockOver = blockedUntil == null || !now.isBefore(blockedUntil);
            return windowOver && blockOver;
        }
    }

    /** 실패 1회를 원자적으로 더한다. 이번에 차단이 시작됐으면 true. */
    private boolean hit(String key, Limit limit, Instant now) {
        boolean[] startedBlock = {false};
        counters.compute(key, (k, cur) -> {
            if (cur == null || cur.expired(now, limit)) {
                cur = new Counter(now, 0, null);
            }
            if (cur.blockedUntil != null && now.isBefore(cur.blockedUntil)) {
                return cur; // 이미 차단 중 — 차단 시간을 늘리지 않는다(무한 연장 방지)
            }
            int failures = cur.failures + 1;
            if (failures >= limit.maxFailures()) {
                startedBlock[0] = true;
                // 차단이 끝나면 새 창에서 다시 센다
                return new Counter(now, 0, now.plus(limit.block()));
            }
            return new Counter(cur.windowStart, failures, null);
        });
        return startedBlock[0];
    }

    private Duration remaining(String key, Instant now) {
        Counter c = counters.get(key);
        if (c == null || c.blockedUntil == null || !now.isBefore(c.blockedUntil)) return Duration.ZERO;
        return Duration.between(now, c.blockedUntil);
    }

    private boolean isTrusted(String account, String ip, Instant now) {
        Instant at = trustedPairs.get(account + "|" + ip);
        return at != null && now.isBefore(at.plus(trustedIpTtl));
    }

    private void maybeSweep(Instant now) {
        boolean over = counters.size() > MAX_TRACKED_KEYS || trustedPairs.size() > MAX_TRACKED_KEYS;
        if (!over && operations.incrementAndGet() % SWEEP_EVERY != 0) return;

        counters.entrySet().removeIf(e -> e.getValue().expired(now, limitFor(e.getKey())));
        trustedPairs.entrySet().removeIf(e -> !now.isBefore(e.getValue().plus(trustedIpTtl)));

        // 만료된 것을 치워도 넘치면, 차단 중이 아닌 카운터부터 버린다(차단은 유지).
        if (counters.size() > MAX_TRACKED_KEYS) {
            counters.entrySet().removeIf(e -> e.getValue().blockedUntil == null);
        }
        if (trustedPairs.size() > MAX_TRACKED_KEYS) {
            trustedPairs.clear();
        }
    }

    private Limit limitFor(String key) {
        if (key.startsWith("ai:")) return accountIpLimit;
        if (key.startsWith("a:")) return accountLimit;
        return ipLimit;
    }

    private static String accountIpKey(String account, String ip) { return "ai:" + account + "|" + ip; }
    private static String accountKey(String account) { return "a:" + account; }
    private static String ipKey(String ip) { return "i:" + ip; }

    /** 대소문자·앞뒤 공백만 다른 이메일로 한도를 피하지 못하게 정규화한다. */
    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static String mask(String account) {
        int at = account.indexOf('@');
        String local = at < 0 ? account : account.substring(0, at);
        String domain = at < 0 ? "" : account.substring(at);
        String head = local.length() <= 2 ? local : local.substring(0, 2);
        return head + "***" + domain;
    }

    private static Duration max(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
