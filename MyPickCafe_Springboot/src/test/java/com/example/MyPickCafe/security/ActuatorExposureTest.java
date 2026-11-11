package com.example.MyPickCafe.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Actuator 가 관리 포트에서만, 필요한 엔드포인트만 열리는지 검증한다.
 *
 * <p>메인 포트(8080)는 Caddy 가 인터넷에 내보내는 곳이다. 지표가 여기 열리면 URL 목록·메모리·DB 풀 상태가
 * 외부에 드러난다. 관리 포트(8081)는 compose 내부망의 Prometheus 만 닿으므로 인증 없이 열어 둔다.
 * 실제 포트 두 개를 띄워야 확인할 수 있어 MockMvc 대신 RANDOM_PORT 로 띄운다(관리 포트도 무작위로 잡힌다).
 *
 * <p>Spring Boot 테스트는 기본으로 지표 내보내기를 꺼서 /actuator/prometheus 가 생기지 않는다.
 * 운영과 같은 상태를 보려고 {@link AutoConfigureObservability} 로 켠다(트레이싱은 쓰지 않으므로 끈다).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability(tracing = false)
@ActiveProfiles("test")
class ActuatorExposureTest {

    @Autowired
    private TestRestTemplate rest;

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    private ResponseEntity<String> getFromManagementPort(String path) {
        return rest.getForEntity("http://localhost:" + managementPort + path, String.class);
    }

    @Test
    @DisplayName("관리 포트는 메인 포트와 다르다")
    void managementRunsOnSeparatePort() {
        assertThat(managementPort).isNotEqualTo(serverPort);
    }

    @Test
    @DisplayName("Prometheus 는 관리 포트에서 인증 없이 지표를 가져갈 수 있다")
    void prometheusScrapeIsOpenOnManagementPort() {
        // 응답 시간 histogram 이 생기도록 메인 포트에 요청을 하나 보낸다
        rest.getForEntity("http://localhost:" + serverPort + "/api/cafes", String.class);

        ResponseEntity<String> res = getFromManagementPort("/actuator/prometheus");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 대시보드·알림 규칙이 쓰는 지표가 실제로 나오는지 함께 확인한다
        assertThat(res.getBody())
                .contains("application=\"MyPickCafe\"")
                .contains("http_server_requests_seconds_bucket")
                .contains("uri=\"/api/cafes\"")
                .contains("jvm_memory_used_bytes")
                .contains("hikaricp_connections_active")
                .contains("tomcat_threads_busy_threads");
    }

    @Test
    @DisplayName("health 는 관리 포트에서 상세 정보 없이 상태만 돌려준다")
    void healthShowsStatusOnly() {
        ResponseEntity<String> res = getFromManagementPort("/actuator/health");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"status\"").doesNotContain("components");
    }

    @Test
    @DisplayName("노출하지 않은 Actuator 엔드포인트는 관리 포트에서도 열리지 않는다")
    void otherEndpointsAreNotExposed() {
        for (String path : new String[]{"/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator"}) {
            ResponseEntity<String> res = getFromManagementPort(path);
            assertThat(res.getStatusCode().is2xxSuccessful())
                    .as("%s 는 열리면 안 된다 (HTTP %s)", path, res.getStatusCode())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("메인 포트(인터넷에 나가는 쪽)에는 Actuator 경로가 없다")
    void mainPortDoesNotServeActuator() {
        ResponseEntity<String> res = rest.getForEntity(
                "http://localhost:" + serverPort + "/actuator/prometheus", String.class);

        assertThat(res.getStatusCode().is2xxSuccessful()).isFalse();
        assertThat(res.getBody()).doesNotContain("jvm_memory_used_bytes");
    }
}
