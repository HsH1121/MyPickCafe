package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.PythonTagRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 외부 AI 서버가 죽어 있을 때의 동작(graceful degradation)을 고정한다.
 *
 * <p>태그 분석·챗봇 색인은 부가 기능이므로, AI 서버 장애가 예외로 전파되어
 * 리뷰 작성 같은 핵심 기능을 실패시키면 안 된다.
 *
 * <p>닫혀 있는 포트를 향해 실제로 호출해 실패 경로를 그대로 태운다.
 */
@DisplayName("AI 클라이언트 장애 격리")
class AiClientDegradationTest {

    /** 아무도 listen 하지 않는 포트 — 연결이 즉시 거부된다. */
    private static final String DEAD_SERVER = "http://localhost:19999";

    @Test
    @DisplayName("태그 분석 서버가 죽어 있으면 예외 대신 빈 결과를 반환한다")
    void pythonTagClientReturnsEmptyWhenServerIsDown() {
        PythonTagClient client = new PythonTagClient(WebClient.create(DEAD_SERVER));

        var result = client.analyze(
                PythonTagRequest.builder().reviewId(1L).reviewText("커피가 맛있어요").build());

        assertThat(result)
                .as("호출 실패는 Optional.empty로 흡수되어야 한다")
                .isEmpty();
    }

    @Test
    @DisplayName("챗봇 서버가 죽어 있으면 예외 대신 빈 목록을 반환한다")
    void chatbotClientReturnsEmptyListWhenServerIsDown() {
        ChatbotClient client = new ChatbotClient(WebClient.create(DEAD_SERVER));

        assertThat(client.recommend("조용한 카페 추천해줘"))
                .as("추천 실패는 빈 목록으로 흡수되어야 한다")
                .isEmpty();
    }

    @Test
    @DisplayName("비동기 색인 호출은 서버가 죽어 있어도 호출부로 예외를 던지지 않는다")
    void chatbotIndexingNeverThrowsToCaller() {
        ChatbotClient client = new ChatbotClient(WebClient.create(DEAD_SERVER));

        assertThatCode(() -> client.deleteOneAsync(1L))
                .as("fire-and-forget 색인은 호출부에 영향을 주면 안 된다")
                .doesNotThrowAnyException();
    }
}
