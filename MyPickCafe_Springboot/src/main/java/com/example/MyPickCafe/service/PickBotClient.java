package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.PickBotIndexRequest;
import com.example.MyPickCafe.dto.PickBotResponse;
import com.example.MyPickCafe.support.PickBotUnavailableException;
import com.example.MyPickCafe.support.PickBotUnavailableException.Reason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.codec.CodecException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

/**
 * 추천 픽봇(RAG) 서버(FastAPI) 클라이언트.
 *
 * <p>추천 조회는 사용자가 결과를 기다리므로 동기 호출,
 * 벡터 DB(ChromaDB) 색인은 사용자 응답을 지연시킬 이유가 없으므로 비동기 호출이다.
 */
@Slf4j
@Service
public class PickBotClient {

    private final WebClient webClient;

    public PickBotClient(@Qualifier("pickBotWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 자연어 질의로 카페를 추천받는다.
     *
     * <p>호출 실패를 빈 목록으로 흡수하면 "조건에 맞는 카페 없음"과 구분할 수 없으므로,
     * 실패 원인을 분류해 로그로 남기고 {@link PickBotUnavailableException} 을 던진다.
     *
     * <p>결과가 비었을 때 서버가 이유를 알려주면 {@link PickBotResponse#getNotice()} 에 담긴다.
     *
     * @throws PickBotUnavailableException 픽봇 서버에서 추천 결과를 받지 못한 경우
     */
    public PickBotResponse recommend(String query) {
        long startedAt = System.currentTimeMillis();
        PickBotResponse res;
        try {
            res = webClient.post()
                    .uri("/pickbot/recommend")
                    .bodyValue(Map.of("query", query))
                    .retrieve()
                    .bodyToMono(PickBotResponse.class)
                    .block();
        } catch (Exception e) {
            throw failure(query, System.currentTimeMillis() - startedAt, e);
        }

        long elapsedMs = System.currentTimeMillis() - startedAt;
        if (res == null) {
            res = new PickBotResponse();
        }
        if (res.getResults() == null) {
            res.setResults(new ArrayList<>());
        }
        if (res.getResults().isEmpty()) {
            log.info("픽봇 추천 결과 없음 (서버 정상 응답) [{}ms] query=\"{}\" notice={}",
                    elapsedMs, query, res.getNotice());
        } else {
            log.debug("픽봇 추천 {}건 [{}ms] query=\"{}\"", res.getResults().size(), elapsedMs, query);
        }
        return res;
    }

    private static PickBotUnavailableException failure(String query, long elapsedMs, Exception e) {
        Reason reason = classify(e);
        Throwable root = rootCause(e);
        String cause = root.getClass().getSimpleName()
                + (root.getMessage() != null ? ": " + root.getMessage() : "");

        switch (reason) {
            case CONNECTION_FAILED -> log.warn(
                    "픽봇 추천 실패 - 서버 연결 불가 [{}ms] query=\"{}\" cause={}", elapsedMs, query, cause);
            case TIMEOUT -> log.warn(
                    "픽봇 추천 실패 - 응답 타임아웃 [{}ms] query=\"{}\" cause={}", elapsedMs, query, cause);
            case HTTP_ERROR -> {
                WebClientResponseException re = (WebClientResponseException) e;
                log.warn("픽봇 추천 실패 - 서버 오류 응답 [{}ms] query=\"{}\" status={} body={}",
                        elapsedMs, query, re.getStatusCode().value(), truncate(re.getResponseBodyAsString()));
            }
            case INVALID_RESPONSE -> log.warn(
                    "픽봇 추천 실패 - 응답 형식 오류 [{}ms] query=\"{}\" cause={}", elapsedMs, query, cause);
            default -> log.error(
                    "픽봇 추천 실패 - 분류되지 않은 오류 [{}ms] query=\"{}\"", elapsedMs, query, e);
        }
        return new PickBotUnavailableException(reason, "픽봇 추천 실패: " + reason, e);
    }

    private static Reason classify(Exception e) {
        if (e instanceof WebClientResponseException) {
            return Reason.HTTP_ERROR;
        }
        // WebClientRequestException 등 래퍼 안쪽의 실제 원인으로 판단한다.
        // 타임아웃 예외는 메시지가 null 이라 getMessage() 만으로는 구분할 수 없다.
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof UnknownHostException) {
                return Reason.CONNECTION_FAILED; // netty ConnectTimeoutException 포함
            }
            if (t instanceof io.netty.handler.timeout.TimeoutException
                    || t instanceof java.util.concurrent.TimeoutException) {
                return Reason.TIMEOUT;
            }
            if (t instanceof CodecException) {
                return Reason.INVALID_RESPONSE;
            }
        }
        return Reason.UNKNOWN;
    }

    private static Throwable rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

    private static String truncate(String body) {
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    /**
     * 리뷰 1건을 ChromaDB에 비동기 upsert — 리뷰 작성/수정 시 호출.
     *
     * <p>기존에는 {@code CompletableFuture.runAsync}로 별도 스레드를 점유했지만,
     * WebClient는 논블로킹 I/O라 {@code subscribe()}만으로 스레드 점유 없이
     * 요청을 띄울 수 있다.
     */
    public void indexOneAsync(PickBotIndexRequest req) {
        webClient.post()
                .uri("/pickbot/index-one")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        ignored -> {},
                        e -> log.warn("PickBot index-one 호출 실패 (무시됨): {}", e.getMessage())
                );
    }

    /** 리뷰 1건을 ChromaDB에서 비동기 삭제 — 리뷰 삭제 시 호출. */
    public void deleteOneAsync(Long reviewId) {
        webClient.post()
                .uri("/pickbot/delete-one")
                .bodyValue(Map.of("reviewId", reviewId))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        ignored -> {},
                        e -> log.warn("PickBot delete-one 호출 실패 (무시됨): {}", e.getMessage())
                );
    }
}
