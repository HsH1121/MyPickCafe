package com.example.MyPickCafe.api;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 카페 수정·삭제 API의 소유권 검증 회귀 테스트.
 *
 * <p>수정 전에는 {@code PUT/DELETE /api/cafes/{id}}가 역할(CAFEOWNER)만 확인해서,
 * 점주라면 누구든 다른 점주의 카페를 수정하거나 삭제할 수 있었다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CafeApiOwnershipTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_OWNER_EMAIL = "other-owner@example.com";
    private static final String UPDATE_BODY = """
            {"name":"바뀐이름"}
            """;

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CafeRepository cafeRepository;

    private Long cafeId;

    @BeforeEach
    void setUp() {
        Member owner = saveOwner(OWNER_EMAIL, "owner");
        saveOwner(OTHER_OWNER_EMAIL, "other-owner");

        Cafe cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName("소유권테스트카페");
        cafe.setAddress("서울시 마포구 테스트로 1");
        cafe.setNumber("02-888-8888");
        cafe.setCode("CAFE");
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(0L);
        cafe.setStatus(CafeStatus.APPROVED);
        cafeId = cafeRepository.save(cafe).getId();
    }

    private Member saveOwner(String email, String nickname) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword("$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW");
        m.setNickname(nickname);
        m.setRoleKind(RoleKind.CAFEOWNER);
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    // ===== 수정 =====

    @Test
    @DisplayName("다른 점주는 카페를 수정할 수 없다")
    @WithMockUser(username = OTHER_OWNER_EMAIL, roles = "CAFEOWNER")
    void updateRejectsOtherOwner() throws Exception {
        mvc.perform(put("/api/cafes/{id}", cafeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden());

        assertThat(cafeRepository.findById(cafeId).orElseThrow().getName())
                .isEqualTo("소유권테스트카페");
    }

    @Test
    @DisplayName("해당 카페의 점주는 카페를 수정할 수 있다")
    @WithMockUser(username = OWNER_EMAIL, roles = "CAFEOWNER")
    void updateAllowsOwner() throws Exception {
        mvc.perform(put("/api/cafes/{id}", cafeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk());

        assertThat(cafeRepository.findById(cafeId).orElseThrow().getName())
                .isEqualTo("바뀐이름");
    }

    @Test
    @DisplayName("존재하지 않는 카페 수정은 404를 반환한다")
    @WithMockUser(username = OWNER_EMAIL, roles = "CAFEOWNER")
    void updateMissingCafeReturnsNotFound() throws Exception {
        mvc.perform(put("/api/cafes/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isNotFound());
    }

    // ===== 삭제 =====

    @Test
    @DisplayName("다른 점주는 카페를 삭제할 수 없다")
    @WithMockUser(username = OTHER_OWNER_EMAIL, roles = "CAFEOWNER")
    void deleteRejectsOtherOwner() throws Exception {
        mvc.perform(delete("/api/cafes/{id}", cafeId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertThat(cafeRepository.existsById(cafeId)).isTrue();
    }

    @Test
    @DisplayName("해당 카페의 점주는 카페를 삭제할 수 있다")
    @WithMockUser(username = OWNER_EMAIL, roles = "CAFEOWNER")
    void deleteAllowsOwner() throws Exception {
        mvc.perform(delete("/api/cafes/{id}", cafeId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(cafeRepository.existsById(cafeId)).isFalse();
    }

    @Test
    @DisplayName("관리자는 점주가 아니어도 카페를 삭제할 수 있다")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void deleteAllowsAdmin() throws Exception {
        mvc.perform(delete("/api/cafes/{id}", cafeId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(cafeRepository.existsById(cafeId)).isFalse();
    }
}
