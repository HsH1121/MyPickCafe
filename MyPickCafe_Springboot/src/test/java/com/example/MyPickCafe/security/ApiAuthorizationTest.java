package com.example.MyPickCafe.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig의 인가 규칙이 실제로 강제되는지 검증한다.
 *
 * <p>리팩토링 전에는 {@code GET/POST /api/**}가 전부 permitAll이라
 * 인가 판단이 컨트롤러 내부에 흩어져 있었다. 이 테스트는 그 규칙이
 * 필터 체인에서 선언적으로 강제되는 것을 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    // ===== 공개 엔드포인트 =====

    @Test
    @DisplayName("카페 목록은 비로그인 사용자도 조회할 수 있다")
    void cafeListIsPublic() throws Exception {
        mvc.perform(get("/api/cafes"))
                .andExpect(status().isOk());
    }

    // ===== 회원 정보 (관리자 전용) =====

    @Test
    @DisplayName("회원 목록은 비로그인 사용자가 조회할 수 없다")
    void memberListRejectsAnonymous() throws Exception {
        mvc.perform(get("/api/members").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원 목록은 일반 회원(MEMBER)이 조회할 수 없다")
    @WithMockUser(roles = "MEMBER")
    void memberListRejectsPlainMember() throws Exception {
        mvc.perform(get("/api/members").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("회원 목록은 관리자(ADMIN)만 조회할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void memberListAllowsAdmin() throws Exception {
        mvc.perform(get("/api/members").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ===== 카페 등록 (점주/관리자 전용) =====

    @Test
    @DisplayName("카페 등록은 비로그인 사용자가 호출할 수 없다")
    void cafeCreateRejectsAnonymous() throws Exception {
        mvc.perform(post("/api/cafes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"테스트카페","address":"서울시 마포구",
                                 "phone":"02-000-0000","code":"CAFE"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("카페 등록은 일반 회원(MEMBER)이 호출할 수 없다")
    @WithMockUser(roles = "MEMBER")
    void cafeCreateRejectsPlainMember() throws Exception {
        mvc.perform(post("/api/cafes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"테스트카페","address":"서울시 마포구",
                                 "phone":"02-000-0000","code":"CAFE"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ===== 관리자 페이지 =====

    @Test
    @DisplayName("관리자 API는 일반 회원(MEMBER)이 호출할 수 없다")
    @WithMockUser(roles = "MEMBER")
    void adminApiRejectsPlainMember() throws Exception {
        mvc.perform(get("/admin/cafes/pending").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
