package com.example.MyPickCafe.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Open Session In View(OSIV)를 AI 응답을 기다리는 요청에서만 뺀다.
 *
 * <p>OSIV 는 요청이 끝날 때까지 EntityManager 를 열어 두고, 처음 DB 를 쓴 순간 받은 커넥션을
 * 응답을 다 보낼 때까지 쥐고 있다. 화면이 서비스 밖에서 지연 로딩(예: 메인의 {@code review.cafe.name})을
 * 할 수 있는 대신, 요청 중에 오래 기다리는 일이 있으면 그동안 커넥션도 묶인다.
 *
 * <p>픽봇 추천·리뷰 작성·리뷰 수정은 AI 서버 응답을 요청 스레드에서 수 초~수십 초 기다린다.
 * OSIV 를 켜 두면 느린 요청 20건(prod 커넥션 풀 크기)만으로 풀이 바닥나 AI 와 상관없는 화면까지
 * 커넥션을 기다렸다(docs/MONITORING.md 의 "느린 AI 테스트" 검증 기록: 픽봇 30건에 열람 p95 11ms → 5.71초).
 * 이 경로들은 OSIV 없이 서비스의 트랜잭션 동안만 커넥션을 쓴다.
 *
 * <p>OSIV 를 전부 끄지 않은 이유: 여러 화면·컨트롤러가 서비스가 돌려준 엔티티의 지연 로딩 연관을
 * 트랜잭션 밖에서 읽는다. 전부 끄려면 그 화면들을 DTO 로 바꿔야 해서 따로 진행한다.
 * 그래서 Spring Boot 의 OSIV 자동 등록(spring.jpa.open-in-view)은 끄고, 같은 인터셉터를 여기서
 * 경로를 빼고 등록한다.
 */
@Configuration
public class OpenInViewConfig implements WebMvcConfigurer {

    /**
     * AI 서버 응답을 요청 스레드에서 기다리는 경로. 여기서는 엔티티의 지연 로딩 연관을
     * 서비스 트랜잭션 밖에서 읽으면 안 된다(LazyInitializationException).
     */
    static final String[] PATHS_WAITING_ON_AI = {
            "/api/pickbot/**",   // PickBotController.recommend → PickBotClient.recommend
            "/reviews/new",      // ReviewService.saveWithTags → PythonTagClient.analyze
            "/reviews/*/edit",   // ReviewService.updateWithTags → PythonTagClient.analyze
    };

    private final EntityManagerFactory entityManagerFactory;

    public OpenInViewConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        OpenEntityManagerInViewInterceptor openInView = new OpenEntityManagerInViewInterceptor();
        openInView.setEntityManagerFactory(entityManagerFactory);
        registry.addWebRequestInterceptor(openInView).excludePathPatterns(PATHS_WAITING_ON_AI);
    }
}
