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

    @Value("${python.api.base-url}")
    private String pythonApiBaseUrl;

    @Value("${chatbot.api.base-url}")
    private String chatbotApiBaseUrl;

    /** 리뷰 태그·감성 분석 서버 전용 WebClient. */
    @Bean
    public WebClient pythonTagWebClient(WebClient.Builder builder) {
        return builder.baseUrl(pythonApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient()))
                .build();
    }

    /** 추천 챗봇(RAG) 서버 전용 WebClient. */
    @Bean
    public WebClient chatbotWebClient(WebClient.Builder builder) {
        return builder.baseUrl(chatbotApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient()))
                .build();
    }

    private HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));
    }
}
