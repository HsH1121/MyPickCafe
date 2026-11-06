package com.example.MyPickCafe.api;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import com.example.MyPickCafe.service.NotificationService;
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
 * 알림 목록 응답에 불필요한 민감정보가 실리지 않는지 검증한다.
 *
 * <p>수정 전에는 {@code GET /api/notifications}가 Notification 엔티티를 그대로 반환해
 * recipient·cafe·review 연관을 타고 점주 본인의 tokenVersion, 사업자 증빙 경로(bizDoc),
 * 그리고 리뷰 작성자(타인)의 이메일·나이·성별까지 응답에 실렸다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationResponseLeakTest {

    private static final String OWNER_EMAIL = "noti-owner@example.com";
    private static final String REVIEWER_EMAIL = "noti-reviewer@example.com";
    private static final String BIZ_DOC_KEY = "cafes/999/docs/secret-bizdoc.pdf";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CafeRepository cafeRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        Member owner = member(OWNER_EMAIL, "noti-owner", RoleKind.CAFEOWNER, 41L, "F");
        Member reviewer = member(REVIEWER_EMAIL, "noti-reviewer", RoleKind.MEMBER, 27L, "M");

        Cafe cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName("알림누출테스트카페");
        cafe.setAddress("서울시 마포구 테스트로 2");
        cafe.setNumber("02-888-8888");
        cafe.setCode("CAFE");
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(0L);
        cafe.setStatus(CafeStatus.APPROVED);
        cafe.setBizDoc(BIZ_DOC_KEY);
        cafe = cafeRepository.save(cafe);

        Review review = new Review();
        review.setCafe(cafe);
        review.setMember(reviewer);
        review.setContent("알림 누출 테스트 리뷰");
        review.setCreatedAt(LocalDateTime.now());
        review = reviewRepository.save(review);

        notificationService.notifyReviewCreated(review);
    }

    private Member member(String email, String nickname, RoleKind role, Long age, String gender) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(TestPasswords.HASH);
        m.setNickname(nickname);
        m.setAge(age);
        m.setGender(gender);
        m.setRoleKind(role);
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    @Test
    @DisplayName("알림 목록에는 화면에 필요한 필드만 실리고 연관 엔티티 정보는 나가지 않는다")
    @WithMockUser(username = OWNER_EMAIL, roles = "CAFEOWNER")
    void notificationsExposeOnlyDisplayFields() throws Exception {
        String body = mvc.perform(get("/api/notifications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].read").value(false))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("리뷰 작성자(타인)의 이메일이 점주에게 나가면 안 된다")
                .doesNotContain(REVIEWER_EMAIL);
        assertThat(body)
                .as("수신자 본인 이메일·사업자 증빙 경로·tokenVersion도 알림에 필요 없다")
                .doesNotContain(OWNER_EMAIL)
                .doesNotContain("secret-bizdoc")
                .doesNotContain("tokenVersion");

        mvc.perform(get("/api/notifications").accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].recipient").doesNotExist())
                .andExpect(jsonPath("$[0].cafe").doesNotExist())
                .andExpect(jsonPath("$[0].review").doesNotExist());
    }
}
