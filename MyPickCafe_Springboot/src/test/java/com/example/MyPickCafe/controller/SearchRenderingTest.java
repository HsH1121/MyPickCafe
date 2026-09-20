package com.example.MyPickCafe.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 검색 결과가 실제로 화면에 렌더링되는지 고정한다.
 *
 * <p>검색은 결과를 찾고도 화면에는 아무것도 보이지 않았다. 컨트롤러가 결과를
 * {@code trendingCafes}(엔티티 목록)로 넣었는데 {@code page/main}은
 * {@code cafeCards}(CafeCardForm)만 렌더링했기 때문이다. 쿼리만 검증하는 테스트로는
 * 잡히지 않으므로, 뷰가 읽는 모델 이름과 타입까지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("검색 결과 렌더링")
class SearchRenderingTest {

    @Autowired private MockMvc mvc;
    @Autowired private CafeRepository cafeRepository;
    @Autowired private MemberRepository memberRepository;

    private Member owner;

    @BeforeEach
    void setUp() {
        owner = new Member();
        owner.setEmail("search-test@example.com");
        owner.setPassword("$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW");
        owner.setNickname("search-test-owner");
        owner.setAge(30L);
        owner.setGender("F");
        owner.setRoleKind(RoleKind.CAFEOWNER);
        owner.setTokenVersion(0L);
        owner = memberRepository.save(owner);

        cafeRepository.save(cafe("검색테스트 커피로스터스", "서울시 마포구 서교동 1-1", "02-900-0001", CafeStatus.APPROVED));
        cafeRepository.save(cafe("검색테스트 승인대기 카페", "서울시 마포구 커피길 2-2", "02-900-0002", CafeStatus.PENDING));
    }

    @Test
    @DisplayName("이름이 일치하는 승인된 카페가 cafeCards 로 전달된다")
    void searchPutsResultsIntoCafeCards() throws Exception {
        mvc.perform(get("/search").param("q", "커피로스터스"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cafeCards", hasSize(1)));
    }

    @Test
    @DisplayName("승인되지 않은 카페는 검색되지 않는다")
    void pendingCafeIsNotSearchable() throws Exception {
        mvc.perform(get("/search").param("q", "승인대기"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cafeCards", hasSize(0)));
    }

    @Test
    @DisplayName("검색어가 없으면 승인된 카페 목록을 보여준다")
    void blankQueryListsApprovedCafes() throws Exception {
        mvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cafeCards", hasSize(1)));
    }

    private Cafe cafe(String name, String address, String phone, CafeStatus status) {
        Cafe c = new Cafe();
        c.setOwner(owner);
        c.setName(name);
        c.setAddress(address);
        c.setNumber(phone);
        c.setCode("CAFE");
        c.setDate(LocalDateTime.now());
        c.setViews(0L);
        c.setStatus(status);
        return c;
    }
}
