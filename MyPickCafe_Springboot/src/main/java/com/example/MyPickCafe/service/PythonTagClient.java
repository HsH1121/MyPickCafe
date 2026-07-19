package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.PythonTagRequest;
import com.example.MyPickCafe.dto.PythonTagResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/**
 * 리뷰 태그·감성 분석 AI 서버(FastAPI) 클라이언트.
 *
 * <p>태그 분석은 <b>부가 기능</b>이다. 서버가 죽었거나 느리더라도
 * 리뷰 작성 자체는 성공해야 하므로, 실패를 예외로 전파하지 않고
 * {@link Optional#empty()}로 돌려 호출부가 태그 없이 진행하게 한다.
 */
@Slf4j
@Service
public class PythonTagClient {

    private final WebClient webClient;

    public PythonTagClient(@Qualifier("pythonTagWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * {@code POST /review/analyze} 호출.
     *
     * <p>서블릿 스택이므로 결과를 {@code block()}으로 받는다.
     * 무한 대기를 막는 타임아웃은 {@code AiClientConfig}에서 커넥터에 걸어 두었다.
     */
    public Optional<PythonTagResponse> analyze(PythonTagRequest req) {
        try {
            PythonTagResponse res = webClient.post()
                    .uri("/review/analyze")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(PythonTagResponse.class)
                    .block();
            return Optional.ofNullable(res);
        } catch (Exception e) {
            log.warn("Python tag API 호출 실패 (무시하고 태그 없이 진행): {}", e.getMessage());
            return Optional.empty();
        }
    }
}
