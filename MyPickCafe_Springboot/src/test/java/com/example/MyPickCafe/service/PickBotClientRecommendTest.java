package com.example.MyPickCafe.service;

import com.example.MyPickCafe.support.PickBotUnavailableException;
import com.example.MyPickCafe.support.PickBotUnavailableException.Reason;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 픽봇 추천 조회의 결과 / 실패 구분을 고정한다.
 *
 * <p>예전에는 모든 실패를 빈 목록으로 흡수해, 화면에서 "조건에 맞는 카페 없음"과
 * "추천 서버 장애"를 구분할 수 없었고 로그에도 원인이 남지 않았다(타임아웃은 {@code null}).
 * 실패 유형마다 실제 HTTP 서버를 띄워 {@link Reason} 분류를 검증한다.
 */
@DisplayName("픽봇 추천 조회 결과/실패 구분")
class PickBotClientRecommendTest {

    private static final String QUERY = "조용한 카페 추천해줘";

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("서버가 빈 배열로 정상 응답하면 예외 없이 빈 목록을 반환한다")
    void emptyResultIsNotAFailure() {
        respond(200, "[]", 0);

        assertThat(client(Duration.ofSeconds(5)).recommend(QUERY)).isEmpty();
    }

    @Test
    @DisplayName("서버가 결과를 주면 그대로 반환한다")
    void returnsResults() {
        respond(200, "[{\"cafeId\":1,\"cafeName\":\"카페\",\"address\":\"서울\",\"snippet\":\"조용해요\",\"score\":0.9}]", 0);

        assertThat(client(Duration.ofSeconds(5)).recommend(QUERY))
                .singleElement()
                .satisfies(r -> assertThat(r.getCafeId()).isEqualTo(1L));
    }

    @Test
    @DisplayName("서버가 떠 있지 않으면 CONNECTION_FAILED")
    void connectionRefused() {
        PickBotClient client = new PickBotClient(WebClient.create("http://localhost:19999"));

        assertReason(() -> client.recommend(QUERY), Reason.CONNECTION_FAILED);
    }

    @Test
    @DisplayName("응답 제한 시간을 넘기면 TIMEOUT")
    void timeout() {
        respond(200, "[]", 2_000);

        assertReason(() -> client(Duration.ofMillis(300)).recommend(QUERY), Reason.TIMEOUT);
    }

    @Test
    @DisplayName("서버가 5xx 로 응답하면 HTTP_ERROR")
    void serverError() {
        respond(503, "{\"detail\":\"RAG 모듈이 초기화되지 않았습니다.\"}", 0);

        assertReason(() -> client(Duration.ofSeconds(5)).recommend(QUERY), Reason.HTTP_ERROR);
    }

    @Test
    @DisplayName("응답 본문을 결과로 변환할 수 없으면 INVALID_RESPONSE")
    void invalidBody() {
        respond(200, "[{\"cafeId\":\"not-a-number\"}]", 0);

        assertReason(() -> client(Duration.ofSeconds(5)).recommend(QUERY), Reason.INVALID_RESPONSE);
    }

    private PickBotClient client(Duration responseTimeout) {
        HttpClient httpClient = HttpClient.create().responseTimeout(responseTimeout);
        return new PickBotClient(WebClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build());
    }

    private void respond(int status, String body, long delayMs) {
        server.createContext("/pickbot/recommend", exchange -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            } catch (IOException ignored) {
                // 타임아웃 테스트에서는 클라이언트가 먼저 끊는다
            }
        });
    }

    private static void assertReason(Runnable call, Reason expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(PickBotUnavailableException.class,
                        e -> assertThat(e.getReason()).isEqualTo(expected));
    }
}
