package com.example.MyPickCafe.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("로그인 실패 횟수 제한 규칙")
class LoginAttemptServiceTest {

    /** 테스트에서 시간을 앞으로 돌릴 수 있는 시계 */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-02T00:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private LoginAttemptService svc;

    private static final String VICTIM = "victim@example.com";

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        svc = new LoginAttemptService(clock,
                new LoginAttemptService.Limit(5, Duration.ofMinutes(5), Duration.ofMinutes(5)),
                new LoginAttemptService.Limit(20, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                new LoginAttemptService.Limit(50, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                Duration.ofDays(30));
    }

    private void fail(String email, String ip, int times) {
        for (int i = 0; i < times; i++) svc.recordFailure(email, ip);
    }

    @Test
    @DisplayName("같은 계정·IP 에서 5분 안에 5번 실패하면 막히고, 5분 뒤 풀린다")
    void blocksAfterFiveFailuresAndExpires() {
        fail(VICTIM, "1.1.1.1", 4);
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isEmpty();

        svc.recordFailure(VICTIM, "1.1.1.1");
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isPresent();

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isEmpty();
    }

    @Test
    @DisplayName("5분 창이 지나면 실패 횟수를 새로 센다")
    void windowResets() {
        fail(VICTIM, "1.1.1.1", 4);
        clock.advance(Duration.ofMinutes(6));
        fail(VICTIM, "1.1.1.1", 4);
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isEmpty();
    }

    @Test
    @DisplayName("이메일 대소문자·공백을 바꿔도 같은 계정으로 센다")
    void normalizesEmail() {
        fail("Victim@Example.com", "1.1.1.1", 3);
        fail(" victim@example.com ", "1.1.1.1", 2);
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isPresent();
    }

    @Test
    @DisplayName("한 IP 에서 막혀도 다른 IP 의 실제 사용자는 로그인할 수 있다")
    void accountIpBlockDoesNotAffectOtherIps() {
        fail(VICTIM, "6.6.6.6", 5);
        assertThat(svc.blockedFor(VICTIM, "6.6.6.6")).isPresent();
        assertThat(svc.blockedFor(VICTIM, "2.2.2.2")).isEmpty();
    }

    @Test
    @DisplayName("여러 IP 로 나눠 한 계정에 20번 실패하면 계정 전체가 막힌다")
    void distributedAttackBlocksAccount() {
        for (int i = 0; i < 20; i++) svc.recordFailure(VICTIM, "10.0.0." + i);
        assertThat(svc.blockedFor(VICTIM, "9.9.9.9")).isPresent();
    }

    @Test
    @DisplayName("계정 전체 차단 중에도 최근 로그인에 성공한 IP 는 막히지 않는다(제삼자 잠금 완화)")
    void trustedIpBypassesAccountWideBlock() {
        svc.recordSuccess(VICTIM, "2.2.2.2");
        for (int i = 0; i < 20; i++) svc.recordFailure(VICTIM, "10.0.0." + i);

        assertThat(svc.blockedFor(VICTIM, "9.9.9.9")).as("처음 보는 IP").isPresent();
        assertThat(svc.blockedFor(VICTIM, "2.2.2.2")).as("로그인 이력이 있는 IP").isEmpty();
    }

    @Test
    @DisplayName("한 IP 에서 여러 계정을 돌아가며 50번 실패하면 IP 가 막힌다")
    void credentialStuffingBlocksIp() {
        for (int i = 0; i < 50; i++) svc.recordFailure("user" + i + "@example.com", "7.7.7.7");
        assertThat(svc.blockedFor("someone-else@example.com", "7.7.7.7")).isPresent();
        assertThat(svc.blockedFor("someone-else@example.com", "8.8.8.8")).isEmpty();
    }

    @Test
    @DisplayName("NAT 뒤 여러 사용자가 몇 번씩 틀리는 정도로는 IP 가 막히지 않는다")
    void sharedIpWithOccasionalTyposIsNotBlocked() {
        for (int u = 0; u < 15; u++) fail("user" + u + "@example.com", "100.64.0.1", 2); // 30회
        assertThat(svc.blockedFor("newcomer@example.com", "100.64.0.1")).isEmpty();
    }

    @Test
    @DisplayName("차단 중 계속 시도해도 차단 시간이 늘어나지 않는다")
    void blockIsNotExtendedByFurtherAttempts() {
        fail(VICTIM, "1.1.1.1", 5);
        clock.advance(Duration.ofMinutes(4));
        fail(VICTIM, "1.1.1.1", 10);
        clock.advance(Duration.ofMinutes(1).plusSeconds(1));
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isEmpty();
    }

    @Test
    @DisplayName("성공하면 그 계정·IP 의 실패 기록이 지워진다")
    void successResetsAccountIpCounter() {
        fail(VICTIM, "1.1.1.1", 4);
        svc.recordSuccess(VICTIM, "1.1.1.1");
        fail(VICTIM, "1.1.1.1", 4);
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isEmpty();
    }

    @Test
    @DisplayName("동시에 실패가 몰려도 횟수를 빠뜨리지 않는다")
    void countsConcurrentFailures() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 5; i++) {
            pool.submit(() -> { start.await(); svc.recordFailure(VICTIM, "1.1.1.1"); return null; });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(svc.blockedFor(VICTIM, "1.1.1.1")).isPresent();
    }

    @Test
    @DisplayName("로그용 마스킹은 이메일 앞 두 글자와 도메인만 남긴다")
    void masksEmail() {
        assertThat(LoginAttemptService.mask("victim@example.com")).isEqualTo("vi***@example.com");
    }
}
