package com.example.MyPickCafe.api;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/auth/me} 응답에 내부 필드가 실리지 않는지 검증한다.
 *
 * <p>수정 전에는 입력 폼 DTO(MemberForm)를 응답에 재사용해 tokenVersion과
 * (값은 null이지만) password 키를 함께 내보냈다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MeResponseLeakTest {

    private static final String OWNER_EMAIL = "me-leak@example.com";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        Member m = new Member();
        m.setEmail(OWNER_EMAIL);
        m.setPassword("$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW");
        m.setNickname("noti-owner");
        m.setAge(41L);
        m.setGender("F");
        m.setRoleKind(RoleKind.CAFEOWNER);
        m.setTokenVersion(0L);
        memberRepository.save(m);
    }

    @Test
    @DisplayName("/api/auth/me 응답에 tokenVersion과 password 키가 없다")
    @WithMockUser(username = OWNER_EMAIL, roles = "CAFEOWNER")
    void meDoesNotExposeInternalFields() throws Exception {
        mvc.perform(get("/api/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OWNER_EMAIL))
                .andExpect(jsonPath("$.nickname").value("noti-owner"))
                .andExpect(jsonPath("$.tokenVersion").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
