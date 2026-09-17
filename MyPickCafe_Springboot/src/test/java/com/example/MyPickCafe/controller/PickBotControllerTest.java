package com.example.MyPickCafe.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 픽봇 서버 장애가 "추천 결과 없음(200 [])"으로 위장되지 않는지 검증한다.
 *
 * <p>test 프로파일의 픽봇 서버 주소는 닫힌 포트라 호출이 즉시 실패한다.
 * 화면은 503 을 받아야 "일시적인 오류" 문구를 띄울 수 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PickBotControllerTest {

    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("픽봇 서버 호출이 실패하면 503 과 실패 사유를 반환한다")
    void returns503WhenPickBotServerIsDown() throws Exception {
        mvc.perform(post("/api/pickbot/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"조용한 카페\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.reason").value("CONNECTION_FAILED"));
    }
}
