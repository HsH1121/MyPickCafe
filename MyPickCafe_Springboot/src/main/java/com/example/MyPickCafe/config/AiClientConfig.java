package com.example.MyPickCafe.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 외부 AI 서버(FastAPI) 호출용 WebClient 설정.
 *
 * <p>기존에는 옵션 없는 {@code new RestTemplate()}을 공유했다. 이 경우
 * <b>타임아웃이 무제한</b>이라 AI 서버가 응답하지 않고 붙잡고 있으면
 * 리뷰 작성 요청 스레드가 그대로 묶여, 부가 기능의 장애가 핵심 기능의
 * 장애로 번진다.
 *
 * <p>여기서는 연결/응답 타임아웃을 명시해 "빠르게 실패하고 태그 없이 진행"
 * 하도록 만든다. 타임아웃 값은 프로파일에서 조정할 수 있다.
 */
@Configuration
public class AiClientConfig {

    @Value("${ai.client.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${ai.client.read-timeout-ms}")
    private int readTimeoutMs;

    /**
     * 픽봇 추천은 임베딩 검색 뒤 LLM 생성까지 기다려야 해서 태그 분석보다 훨씬 오래 걸린다.
     * 공용 응답 타임아웃(10초)을 쓰면 LLM 이 답하기 전에 끊겨 추천이 실패하므로 따로 둔다.
     */
    @Value("${ai.pickbot.read-timeout-ms}")
    private int pickBotReadTimeoutMs;

    @Value("${python.api.base-url}")
    private String pythonApiBaseUrl;

    @Value("${pickbot.api.base-url}")
    private String pickBotApiBaseUrl;

    /**
     * AI 서버가 Spring 의 호출인지 확인하는 공유 토큰(X-Internal-Token).
     * AI 서버의 엔드포인트에는 사용자 인증이 없으므로, 포트를 닫는 것과 별개로 이 토큰을 맞춰 둔다.
     * 비어 있으면 헤더를 보내지 않는다(로컬 개발).
     */
    @Value("${ai.internal-token:}")
    private String internalToken;

    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    /** 리뷰 태그·감성 분석 서버 전용 WebClient. */
    @Bean
    public WebClient pythonTagWebClient(WebClient.Builder builder) {
        return withInternalToken(builder.baseUrl(pythonApiBaseUrl))
                .clientConnector(new ReactorClientHttpConnector(httpClient(readTimeoutMs)))
                .build();
    }

    /** 추천 픽봇(RAG) 서버 전용 WebClient. */
    @Bean
    public WebClient pickBotWebClient(WebClient.Builder builder) {
        return withInternalToken(builder.baseUrl(pickBotApiBaseUrl))
                .clientConnector(new ReactorClientHttpConnector(httpClient(pickBotReadTimeoutMs)))
                .build();
    }

    private WebClient.Builder withInternalToken(WebClient.Builder builder) {
        if (internalToken != null && !internalToken.isBlank()) {
            builder.defaultHeader(INTERNAL_TOKEN_HEADER, internalToken.trim());
        }
        return builder;
    }

    private HttpClient httpClient(int readTimeoutMs) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));
    }
}
