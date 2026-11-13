package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.dto.PickBotResponse;
import com.example.MyPickCafe.dto.PythonTagResponse;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import com.example.MyPickCafe.service.PickBotClient;
import com.example.MyPickCafe.service.PythonTagClient;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 서버 응답을 기다리는 동안 DB 커넥션을 잡고 있지 않은지 확인한다.
 *
 * <p>픽봇 추천·리뷰 작성·리뷰 수정은 AI 응답을 요청 스레드에서 기다린다(수 초~수십 초).
 * 그동안 커넥션을 쥐고 있으면 느린 요청 20건(prod 풀 크기)만으로 커넥션 풀이 바닥나
 * AI 와 상관없는 화면까지 커넥션을 기다린다(docs/MONITORING.md 의 "느린 AI 테스트").
 * AI 클라이언트를 가짜로 바꿔, 호출되는 순간 Hikari 풀에서 빌려 간 커넥션 수를 기록한다.
 *
 * <p>테스트 트랜잭션(@Transactional)을 쓰지 않는다. 테스트 트랜잭션이 요청 내내 커넥션을 잡고 있으면
 * 확인하려는 것을 확인할 수 없다. 대신 데이터를 실제로 저장하고 끝나면 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AI 응답을 기다리는 동안 DB 커넥션을 놓는다")
class DbConnectionDuringAiCallTest {

    private static final String OWNER = "ai-wait-owner@example.com";
    private static final String WRITER = "ai-wait-writer@example.com";

    @Autowired private MockMvc mvc;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CafeRepository cafeRepository;
    @Autowired private ReviewRepository reviewRepository;

    @MockitoBean private PickBotClient pickBotClient;
    @MockitoBean private PythonTagClient pythonTagClient;

    /** AI 클라이언트가 불린 순간 빌려 간 커넥션 수. 불리지 않았으면 -1. */
    private final AtomicInteger activeDuringAiCall = new AtomicInteger(-1);

    private Long cafeId;
    private Long writerId;

    @BeforeEach
    void setUp() {
        Member owner = member(OWNER, "aiwaitowner");
        writerId = member(WRITER, "aiwaitwriter").getId();
        cafeId = cafe(owner).getId();

        when(pickBotClient.recommend(anyString(), anyCollection())).thenAnswer(inv -> {
            activeDuringAiCall.set(activeConnections());
            return new PickBotResponse();
        });
        when(pythonTagClient.analyze(any())).thenAnswer(inv -> {
            activeDuringAiCall.set(activeConnections());
            PythonTagResponse res = new PythonTagResponse();
            res.setSentiment("GOOD");
            res.setFacilityTags(List.of("WIFI"));
            return Optional.of(res);
        });
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM notification WHERE cafe_id = ?", cafeId);
        jdbc.update("DELETE FROM review_tag WHERE review_id IN (SELECT review_id FROM review WHERE cafe_id = ?)", cafeId);
        jdbc.update("DELETE FROM review WHERE cafe_id = ?", cafeId);
        jdbc.update("DELETE FROM cafe_tag WHERE cafe_id = ?", cafeId);
        jdbc.update("DELETE FROM cafe WHERE cafe_id = ?", cafeId);
        jdbc.update("DELETE FROM member WHERE email IN (?, ?)", OWNER, WRITER);
    }

    @Test
    @DisplayName("픽봇 추천: 승인된 카페 ID 를 조회한 뒤 AI 응답을 기다리는 동안 커넥션을 쥐고 있지 않다")
    void pickBotReleasesConnectionWhileWaiting() throws Exception {
        mvc.perform(post("/api/pickbot/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"조용한 카페\"}"))
                .andExpect(status().isOk());

        assertThat(activeDuringAiCall.get()).as("AI 호출 중 빌려 간 커넥션 수").isZero();
    }

    @Test
    @DisplayName("리뷰 작성: 리뷰를 커밋한 뒤 트랜잭션 밖에서 태그 분석을 기다리고, 결과는 그다음에 반영한다")
    void reviewCreateReleasesConnectionWhileWaiting() throws Exception {
        mvc.perform(post("/reviews/new").with(writer())
                        .param("cafeId", String.valueOf(cafeId))
                        .param("reviewContent", "와이파이가 빨라요"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cafes/" + cafeId));

        assertThat(activeDuringAiCall.get()).as("AI 호출 중 빌려 간 커넥션 수").isZero();
        Review saved = reviewRepository.findByCafe_IdOrderByCreatedAtDesc(cafeId).get(0);
        assertTagsApplied(saved);
    }

    @Test
    @DisplayName("리뷰 수정: 본인 확인·내용 수정을 커밋한 뒤 트랜잭션 밖에서 태그 분석을 기다린다")
    void reviewEditReleasesConnectionWhileWaiting() throws Exception {
        Long reviewId = review("처음 쓴 리뷰");

        mvc.perform(post("/reviews/" + reviewId + "/edit").with(writer())
                        .param("cafeId", String.valueOf(cafeId))
                        .param("reviewContent", "다시 와 보니 와이파이가 빨라요"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cafes/" + cafeId));

        assertThat(activeDuringAiCall.get()).as("AI 호출 중 빌려 간 커넥션 수").isZero();
        Review saved = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(saved.getContent()).isEqualTo("다시 와 보니 와이파이가 빨라요");
        assertTagsApplied(saved);
    }

    @Test
    @DisplayName("리뷰 작성: 태그 반영이 실패해도(중복 태그로 유니크 위반) 저장된 리뷰로 정상 응답한다")
    void reviewCreateSurvivesTagApplyFailure() throws Exception {
        when(pythonTagClient.analyze(any())).thenAnswer(inv -> {
            PythonTagResponse res = new PythonTagResponse();
            res.setSentiment("GOOD");
            res.setFacilityTags(List.of("WIFI", "WIFI")); // review_tag (review_id, category_code, code) 유니크 위반
            return Optional.of(res);
        });

        mvc.perform(post("/reviews/new").with(writer())
                        .param("cafeId", String.valueOf(cafeId))
                        .param("reviewContent", "와이파이 와이파이"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cafes/" + cafeId));

        // 리뷰는 남고, 태그 반영 트랜잭션만 통째로 롤백된다(AI 분석이 실패했을 때와 같은 상태)
        Review saved = reviewRepository.findByCafe_IdOrderByCreatedAtDesc(cafeId).get(0);
        assertThat(saved.getContent()).isEqualTo("와이파이 와이파이");
        assertThat(saved.getSentiment()).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_tag WHERE review_id = ?", Integer.class, saved.getId()))
                .isZero();
    }

    private void assertTagsApplied(Review review) {
        assertThat(review.getSentiment()).isEqualTo("GOOD");
        assertThat(review.getGood()).isEqualTo(1);
        assertThat(jdbc.queryForList(
                "SELECT category_code || ':' || code FROM review_tag WHERE review_id = ?", String.class, review.getId()))
                .containsExactly("FACILITY:WIFI");
        assertThat(jdbc.queryForList("SELECT facility_tag FROM cafe_tag WHERE cafe_id = ?", String.class, cafeId))
                .containsExactly("WIFI");
    }

    private int activeConnections() {
        try {
            return dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RequestPostProcessor writer() {
        return user(WRITER).roles("MEMBER");
    }

    private Member member(String email, String nickname) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(TestPasswords.HASH);
        m.setNickname(nickname);
        m.setRoleKind(RoleKind.MEMBER);
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    private Cafe cafe(Member owner) {
        Cafe c = new Cafe();
        c.setOwner(owner);
        c.setName("AI대기테스트카페");
        c.setAddress("서울 마포구 테스트로 1");
        c.setNumber("02-1234");
        c.setCode("CAFE");
        c.setDate(LocalDateTime.now());
        c.setViews(0L);
        c.setStatus(CafeStatus.APPROVED);
        return cafeRepository.save(c);
    }

    private Long review(String content) {
        Review r = new Review();
        r.setCafe(cafeRepository.findById(cafeId).orElseThrow());
        r.setMember(memberRepository.findById(writerId).orElseThrow());
        r.setContent(content);
        r.setGood(0);
        r.setBad(0);
        r.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(r).getId();
    }
}
