package com.example.MyPickCafe.api;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.dto.PickBotResponse;
import com.example.MyPickCafe.dto.PickBotResult;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Menu;
import com.example.MyPickCafe.repository.CafeInfoRepository;
import com.example.MyPickCafe.repository.CafePhotoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.FavoriteRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.repository.MenuRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import com.example.MyPickCafe.service.PickBotClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 승인 전(PENDING)·반려(REJECTED) 카페가 일반 사용자에게 보이지 않는지 확인한다.
 * 점주는 자기 카페를, 관리자는 모든 카페를 볼 수 있어야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("승인 전 카페 노출 차단")
class PendingCafeVisibilityTest {

    private static final String OWNER = "pending-owner@example.com";
    private static final String STRANGER = "stranger@example.com";
    private static final String ADMIN = "admin-reviewer@example.com";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CafeRepository cafeRepository;
    @Autowired private CafeInfoRepository cafeInfoRepository;
    @Autowired private CafePhotoRepository cafePhotoRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private ReviewRepository reviewRepository;

    @MockitoBean private PickBotClient pickBotClient;

    private Long approvedCafeId;
    private Long pendingCafeId;
    private Long rejectedCafeId;
    private Long pendingMenuId;

    @BeforeEach
    void setUp() {
        Member owner = member(OWNER, "pendowner", RoleKind.MEMBER);
        member(STRANGER, "stranger", RoleKind.MEMBER);
        member(ADMIN, "reviewer", RoleKind.ADMIN);

        approvedCafeId = cafe(owner, "승인카페", CafeStatus.APPROVED).getId();
        Cafe pending = cafe(owner, "심사중카페", CafeStatus.PENDING);
        pendingCafeId = pending.getId();
        rejectedCafeId = cafe(owner, "반려카페", CafeStatus.REJECTED).getId();

        CafeInfo info = new CafeInfo();
        info.setCafe(pending);
        info.setNotice("오픈 준비중");
        // 상세 화면은 영업정보 필드가 null 이면 렌더링에 실패한다(기존 동작) — 모두 채워 둔다
        info.setOpenTime("09:00");
        info.setCloseTime("21:00");
        info.setHoliday("없음");
        info.setInfo("소개");
        cafeInfoRepository.save(info);

        CafeInfo approvedInfo = new CafeInfo();
        approvedInfo.setCafe(cafeRepository.findById(approvedCafeId).orElseThrow());
        approvedInfo.setNotice("영업중");
        cafeInfoRepository.save(approvedInfo);

        Menu menu = new Menu();
        menu.setCafe(pending);
        menu.setName("비밀메뉴");
        menu.setPrice(5000);
        pendingMenuId = menuRepository.save(menu).getId();

        CafePhoto photo = new CafePhoto();
        photo.setCafe(pending);
        photo.setUrl("/uploads/cafes/x/secret.jpg");
        photo.setMain(true);
        cafePhotoRepository.save(photo);
    }

    private Member member(String email, String nickname, RoleKind role) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(TestPasswords.HASH);
        m.setNickname(nickname);
        m.setRoleKind(role);
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    private Cafe cafe(Member owner, String name, CafeStatus status) {
        Cafe c = new Cafe();
        c.setOwner(owner);
        c.setName(name);
        c.setAddress("서울 마포구 테스트로 " + name.length());
        c.setNumber("02-" + Math.abs(name.hashCode() % 10000));
        c.setCode("CAFE");
        c.setDate(LocalDateTime.now());
        c.setViews(0L);
        c.setStatus(status);
        return cafeRepository.save(c);
    }

    private static RequestPostProcessor owner() { return user(OWNER).roles("MEMBER"); }
    private static RequestPostProcessor stranger() { return user(STRANGER).roles("MEMBER"); }
    private static RequestPostProcessor admin() { return user(ADMIN).roles("ADMIN", "CAFEOWNER", "MEMBER"); }

    // ── 카페 목록·단건 ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/cafes 는 승인된 카페만 준다")
    void listShowsApprovedOnly() throws Exception {
        mvc.perform(get("/api/cafes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("승인카페")))
                .andExpect(content().string(not(containsString("심사중카페"))))
                .andExpect(content().string(not(containsString("반려카페"))));
    }

    @Test
    @DisplayName("GET /api/cafes/{cafeId}: 비로그인·다른 회원은 PENDING·REJECTED 가 없는 카페와 같은 404")
    void singleIsHiddenFromOthers() throws Exception {
        long missing = 987654321L;
        mvc.perform(get("/api/cafes/{cafeId}", missing)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("카페를 찾을 수 없습니다: " + missing));
        mvc.perform(get("/api/cafes/{cafeId}", pendingCafeId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("카페를 찾을 수 없습니다: " + pendingCafeId));
        mvc.perform(get("/api/cafes/{cafeId}", rejectedCafeId).with(stranger())).andExpect(status().isNotFound());
        mvc.perform(get("/api/cafes/{cafeId}", approvedCafeId)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/cafes/{cafeId}: 점주와 관리자는 승인 전 카페를 볼 수 있다")
    void ownerAndAdminCanSeePending() throws Exception {
        mvc.perform(get("/api/cafes/{cafeId}", pendingCafeId).with(owner())).andExpect(status().isOk());
        mvc.perform(get("/api/cafes/{cafeId}", rejectedCafeId).with(owner())).andExpect(status().isOk());
        mvc.perform(get("/api/cafes/{cafeId}", pendingCafeId).with(admin())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("상세 페이지: 다른 사람은 PENDING 도 리다이렉트 안내 없이 404, 점주는 열람 가능")
    void detailPage() throws Exception {
        mvc.perform(get("/cafes/{cafeId}", pendingCafeId)).andExpect(status().isNotFound());
        mvc.perform(get("/cafes/{cafeId}", pendingCafeId).with(stranger())).andExpect(status().isNotFound());
        mvc.perform(get("/cafes/{cafeId}", pendingCafeId).with(owner())).andExpect(status().isOk());
    }

    // ── 연관 API ───────────────────────────────────────────────────────

    @Test
    @DisplayName("카페 상세정보·메뉴·사진 API 도 승인 전 카페는 점주·관리자만")
    void relatedApis() throws Exception {
        mvc.perform(get("/api/cafe-infos/by-cafe/{cafeId}", pendingCafeId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/menus/by-cafe/{cafeId}", pendingCafeId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/menus/{menuId}", pendingMenuId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/cafes/{cafeId}/photos", pendingCafeId)).andExpect(status().isNotFound());

        mvc.perform(get("/api/cafe-infos/by-cafe/{cafeId}", pendingCafeId).with(owner())).andExpect(status().isOk());
        mvc.perform(get("/api/menus/by-cafe/{cafeId}", pendingCafeId).with(owner())).andExpect(status().isOk());
        mvc.perform(get("/api/menus/{menuId}", pendingMenuId).with(admin())).andExpect(status().isOk());
        mvc.perform(get("/api/cafes/{cafeId}/photos", pendingCafeId).with(admin())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("카페 상세정보 응답에 카페 엔티티(점주 이메일·승인 상태)가 딸려 나가지 않는다")
    void cafeInfoDoesNotEmbedCafe() throws Exception {
        mvc.perform(get("/api/cafe-infos/by-cafe/{cafeId}", approvedCafeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notice").value("영업중"))
                .andExpect(jsonPath("$.cafe").doesNotExist())
                .andExpect(content().string(not(containsString(OWNER))));
    }

    @Test
    @DisplayName("즐겨찾기: 승인 전 카페는 추가할 수 없고, 승인이 취소되면 목록에서 빠진다")
    void favorites() throws Exception {
        mvc.perform(post("/api/favorites/{cafeId}/favorite", pendingCafeId).with(stranger()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/favorites/cafes/{cafeId}/count", pendingCafeId).with(stranger()))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/favorites/{cafeId}/favorite", approvedCafeId).with(stranger()))
                .andExpect(jsonPath("$.favorited").value(true));
        mvc.perform(get("/api/favorites").with(stranger()))
                .andExpect(jsonPath("$.content[0].name").value("승인카페"))
                .andExpect(content().string(not(containsString(OWNER))));

        Cafe c = cafeRepository.findById(approvedCafeId).orElseThrow();
        c.setStatus(CafeStatus.REJECTED);
        cafeRepository.saveAndFlush(c);

        mvc.perform(get("/api/favorites").with(stranger()))
                .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/favorites").with(stranger()))
                .andExpect(content().string(not(containsString("승인카페"))));
        // 해제는 상태와 관계없이 된다
        mvc.perform(post("/api/favorites/{cafeId}/favorite", approvedCafeId).with(stranger()))
                .andExpect(jsonPath("$.favorited").value(false));
    }

    @Test
    @DisplayName("리뷰: 승인 전 카페에는 리뷰를 쓸 수 없다")
    void reviewsOnPendingAreRejected() throws Exception {
        long before = reviewRepository.count();
        mvc.perform(post("/reviews/new").with(stranger())
                        .param("cafeId", String.valueOf(pendingCafeId))
                        .param("content", "몰래 남기는 리뷰"))
                .andExpect(status().isNotFound());
        assertThat(reviewRepository.count()).isEqualTo(before);
        mvc.perform(get("/reviews/cafes/{cafeId}/reviews", pendingCafeId).with(stranger()))
                .andExpect(status().isNotFound());
    }

    // ── 추천 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("픽봇: AI 서버가 승인 전·반려 카페를 돌려줘도 응답에서 뺀다")
    void pickBotFiltersUnapprovedAtTheEnd() throws Exception {
        PickBotResponse fromAi = new PickBotResponse();
        List<PickBotResult> results = new ArrayList<>();
        for (Long cafeId : List.of(pendingCafeId, approvedCafeId, rejectedCafeId)) {
            PickBotResult r = new PickBotResult();
            r.setCafeId(cafeId);
            r.setCafeName("cafe-" + cafeId);
            results.add(r);
        }
        fromAi.setResults(results);
        when(pickBotClient.recommend(anyString())).thenReturn(fromAi);

        mvc.perform(post("/api/pickbot/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"조용한 카페\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].cafeId").value(approvedCafeId));
    }

    @Test
    @DisplayName("기존 동작 유지: 점주는 승인 전 카페 관리 화면에 들어갈 수 있다")
    void ownerCanStillManagePending() throws Exception {
        mvc.perform(get("/cafes/{cafeId}/manage", pendingCafeId).with(owner())).andExpect(status().isOk());
    }
}
