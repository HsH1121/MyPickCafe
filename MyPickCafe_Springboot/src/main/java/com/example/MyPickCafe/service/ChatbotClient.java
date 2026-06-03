package com.example.MyPickCafe.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotClient {

    private final RestTemplate restTemplate;

    @Value("${python.api.base-url}")
    private String baseUrl;

    /**
     * FastAPI POST /chatbot/recommend 를 호출해 추천 카페 목록을 반환한다.
     * 호출 실패 시 빈 리스트를 반환한다.
     */
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
            log.warn("챗봇 API 호출 실패: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
