package com.example.MyPickCafe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 문서 설정.
 *
 * <p>문서 노출 여부는 프로파일이 결정한다.
 * dev에서는 {@code /swagger-ui.html}로 열리고, prod에서는
 * {@code springdoc.api-docs.enabled=false}로 꺼져 API 표면이 공개되지 않는다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI myPickCafeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyPickCafe API")
                        .description("""
                                카페 정보 제공 및 AI 기반 추천 서비스 REST API.

                                인증은 JWT Bearer 토큰을 사용한다.
                                `POST /api/auth/login`으로 발급받은 토큰을
                                우측 상단 Authorize 버튼에 입력하면 인증이 필요한
                                엔드포인트를 그대로 호출해 볼 수 있다.
                                """)
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 응답의 token 값을 입력한다. (Bearer 접두사 불필요)")));
    }
}
