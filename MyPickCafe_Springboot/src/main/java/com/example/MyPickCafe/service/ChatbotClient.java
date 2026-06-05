package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.ChatbotIndexRequest;
import com.example.MyPickCafe.dto.ChatbotResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotClient {

    private final RestTemplate restTemplate;

    @Value("${chatbot.api.base-url}")
    private String baseUrl;

    public List<ChatbotResult> recommend(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("query", query), headers);

            ResponseEntity<List<ChatbotResult>> res = restTemplate.exchange(
                    baseUrl + "/chatbot/recommend",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                return res.getBody();
            }
        } catch (Exception e) {
            log.warn("챗봇 추천 API 호출 실패 (무시됨): {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /** 리뷰 1건을 ChromaDB에 비동기 upsert — 리뷰 작성/수정 시 호출 */
    public void indexOneAsync(ChatbotIndexRequest req) {
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForEntity(baseUrl + "/chatbot/index-one", req, Void.class);
            } catch (Exception e) {
                log.warn("Chatbot index-one 호출 실패 (무시됨): {}", e.getMessage());
            }
        });
    }

    /** 리뷰 1건을 ChromaDB에서 비동기 삭제 — 리뷰 삭제 시 호출 */
    public void deleteOneAsync(Long reviewId) {
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForEntity(baseUrl + "/chatbot/delete-one", Map.of("reviewId", reviewId), Void.class);
            } catch (Exception e) {
                log.warn("Chatbot delete-one 호출 실패 (무시됨): {}", e.getMessage());
            }
        });
    }
}
