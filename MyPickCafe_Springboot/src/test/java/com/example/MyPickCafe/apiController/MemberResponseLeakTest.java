package com.example.MyPickCafe.apiController;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원 비밀번호 해시가 API 응답으로 새어 나가지 않는지 검증하는 회귀 테스트.
 *
 * <p>리팩토링 전에는 컨트롤러가 {@code List<Member>} 엔티티를 그대로 반환했고
 * {@code Member.password}에 직렬화 차단이 없었다. 여기에 더해
 * {@code GET /api/**}가 permitAll이었기 때문에, 인증 없이 전 회원의
 * 이메일과 BCrypt 해시를 덤프할 수 있었다.
 *
 * <p>같은 문제가 {@code Cafe.owner}를 타고 카페 API에서도 발생했으므로
 * 두 경로를 모두 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberResponseLeakTest {

    private static final String BCRYPT_HASH =
            "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CafeRepository cafeRepository;

    private Member owner;

    @BeforeEach
    void setUp() {
        owner = new Member();
        owner.setEmail("leak-test@example.com");
        owner.setPassword(BCRYPT_HASH);
        owner.setNickname("leak-test-owner");
        owner.setAge(30L);
        owner.setGender("F");
        owner.setRoleKind(RoleKind.CAFEOWNER);
        owner.setTokenVersion(0L);
        owner = memberRepository.save(owner);

        Cafe cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName("누출테스트카페");
        cafe.setAddress("서울시 마포구 테스트로 1");
        cafe.setNumber("02-999-9999");
        cafe.setCode("CAFE");
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(0L);
        cafe.setStatus(CafeStatus.APPROVED);
        cafeRepository.save(cafe);
    }

    @Test
    @DisplayName("회원 조회 응답에 비밀번호 해시가 포함되지 않는다")
    @WithMockUser(roles = "ADMIN")
    void memberApiNeverExposesPasswordHash() throws Exception {
        String body = mvc.perform(get("/api/members").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("응답 본문 어디에도 BCrypt 해시가 나타나면 안 된다")
                .doesNotContain(BCRYPT_HASH)
                .doesNotContain("$2a$");
    }

    @Test
    @DisplayName("카페 조회 응답에 점주의 비밀번호·이메일이 포함되지 않는다")
    void cafeApiNeverExposesOwnerCredentials() throws Exception {
        String body = mvc.perform(get("/api/cafes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("카페 응답은 owner 엔티티를 통째로 직렬화하면 안 된다")
                .doesNotContain(BCRYPT_HASH)
                .doesNotContain("$2a$")
                .doesNotContain("leak-test@example.com");

        assertThat(body)
                .as("화면에 필요한 점주 닉네임은 남아 있어야 한다")
                .contains("leak-test-owner");
    }
}
