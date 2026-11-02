package com.example.MyPickCafe.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 서버(FastAPI) 호출에 내부 토큰(X-Internal-Token)이 붙는지 확인한다.
 *
 * <p>AI 서버는 사용자 인증이 없고, AI_INTERNAL_TOKEN 이 설정되면 이 헤더가 없는 요청을 401 로 거부한다.
 * 헤더가 빠지면 리뷰 태그·픽봇이 모두 실패하므로 두 WebClient 모두 확인한다.
 */
@DisplayName("AI 클라이언트 내부 토큰 헤더")
class AiClientInternalTokenTest {

    private HttpServer server;
    private final AtomicReference<String> received = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            // 헤더가 없으면 "<none>" 으로 기록해 "없음"과 "빈 값"을 구분한다
            String token = exchange.getRequestHeaders().getFirst("X-Internal-Token");
            received.set(token == null ? "<none>" : token);
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private AiClientConfig config(String token) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        AiClientConfig cfg = new AiClientConfig();
        ReflectionTestUtils.setField(cfg, "connectTimeoutMs", 1000);
        ReflectionTestUtils.setField(cfg, "readTimeoutMs", 2000);
        ReflectionTestUtils.setField(cfg, "pickBotReadTimeoutMs", 2000);
        ReflectionTestUtils.setField(cfg, "pythonApiBaseUrl", baseUrl);
        ReflectionTestUtils.setField(cfg, "pickBotApiBaseUrl", baseUrl);
        ReflectionTestUtils.setField(cfg, "internalToken", token);
        return cfg;
    }

    private void call(WebClient client) {
        client.post().uri("/review/analyze").bodyValue("{}").retrieve().bodyToMono(String.class).block();
    }

    @Test
    @DisplayName("토큰이 설정되면 태그 분석·픽봇 WebClient 모두 헤더를 보낸다")
    void sendsTokenWhenConfigured() {
        AiClientConfig cfg = config("s3cr3t-token");

        call(cfg.pythonTagWebClient(WebClient.builder()));
        assertThat(received.get()).isEqualTo("s3cr3t-token");

        received.set(null);
        call(cfg.pickBotWebClient(WebClient.builder()));
        assertThat(received.get()).isEqualTo("s3cr3t-token");
    }

    @Test
    @DisplayName("토큰이 비어 있으면 헤더를 보내지 않는다(로컬 개발 호환)")
    void omitsHeaderWhenBlank() {
        call(config("").pythonTagWebClient(WebClient.builder()));
        assertThat(received.get()).isEqualTo("<none>");
    }
}
