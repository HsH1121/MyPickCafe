package com.example.MyPickCafe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OSIV(OpenEntityManagerInViewInterceptor)가 AI 응답을 기다리는 경로에서만 빠지고,
 * 나머지 화면에는 그대로 걸리는지 확인한다. 화면들은 트랜잭션 밖 지연 로딩에 기대고 있어
 * 다른 경로에서 빠지면 LazyInitializationException 으로 깨진다.
 * (WebRequestInterceptor 로 등록하는 것은 OSIV 뿐이라 그 어댑터가 체인에 있는지로 판단한다.)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OSIV 적용 경로")
class OpenInViewConfigTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @ParameterizedTest(name = "{0} {1} → OSIV {2}")
    @CsvSource({
            "GET,  /,                      true",
            "GET,  /cafes,                 true",
            "GET,  /cafes/1,               true",
            "GET,  /member/me,             true",
            "GET,  /reviews/new,           false",
            "POST, /api/pickbot/recommend, false",
            "POST, /reviews/new,           false",
            "POST, /reviews/5/edit,        false",
    })
    void openInViewOnlyOutsideAiWaitingPaths(String method, String path, boolean expected) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        ServletRequestPathUtils.parseAndCache(request);

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertThat(chain).as("%s %s 를 처리할 컨트롤러", method, path).isNotNull();
        boolean openInView = chain.getInterceptorList().stream()
                .anyMatch(WebRequestHandlerInterceptorAdapter.class::isInstance);
        assertThat(openInView).isEqualTo(expected);
    }
}
