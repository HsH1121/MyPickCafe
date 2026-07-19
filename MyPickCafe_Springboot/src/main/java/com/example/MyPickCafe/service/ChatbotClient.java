package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.ChatbotIndexRequest;
import com.example.MyPickCafe.dto.ChatbotResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 추천 챗봇(RAG) 서버(FastAPI) 클라이언트.
 *
 * <p>추천 조회는 사용자가 결과를 기다리므로 동기 호출,
 * 벡터 DB(ChromaDB) 색인은 사용자 응답을 지연시킬 이유가 없으므로 비동기 호출이다.
 */
@Slf4j
@Service
public class ChatbotClient {

    private final WebClient webClient;

    public ChatbotClient(@Qualifier("chatbotWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /** 자연어 질의로 카페를 추천받는다. 실패 시 빈 목록(추천 없음)으로 degrade. */
    public List<ChatbotResult> recommend(String query) {
        try {
            List<ChatbotResult> results = webClient.post()
                    .uri("/chatbot/recommend")
                    .bodyValue(Map.of("query", query))
                    .retrieve()
                    .bodyToFlux(ChatbotResult.class)
                    .collectList()
                    .block();
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.warn("챗봇 추천 API 호출 실패 (빈 결과 반환): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 리뷰 1건을 ChromaDB에 비동기 upsert — 리뷰 작성/수정 시 호출.
     *
     * <p>기존에는 {@code CompletableFuture.runAsync}로 별도 스레드를 점유했지만,
     * WebClient는 논블로킹 I/O라 {@code subscribe()}만으로 스레드 점유 없이
     * 요청을 띄울 수 있다.
     */
    public void indexOneAsync(ChatbotIndexRequest req) {
        webClient.post()
                .uri("/chatbot/index-one")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        ignored -> {},
                        e -> log.warn("Chatbot index-one 호출 실패 (무시됨): {}", e.getMessage())
                );
    }

    /** 리뷰 1건을 ChromaDB에서 비동기 삭제 — 리뷰 삭제 시 호출. */
    public void deleteOneAsync(Long reviewId) {
        webClient.post()
                .uri("/chatbot/delete-one")
                .bodyValue(Map.of("reviewId", reviewId))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        ignored -> {},
                        e -> log.warn("Chatbot delete-one 호출 실패 (무시됨): {}", e.getMessage())
                );
    }
}
