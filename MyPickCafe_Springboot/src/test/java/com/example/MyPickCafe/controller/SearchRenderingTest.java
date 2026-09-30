package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.domain.FacilityTag;
import com.example.MyPickCafe.entity.CafeTag;
import com.example.MyPickCafe.repository.CafeTagRepository;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @Autowired private CafeTagRepository cafeTagRepository;

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

    @Test
    void tagFiltersHaveTheirOwnTab() throws Exception {
        mvc.perform(get("/cafes").param("sort", "tags"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sortTags", true))
                .andExpect(model().attribute("urlSortRecommend", "/cafes?sort=recommend"))
                .andExpect(content().string(containsString("id=\"tagFilters\"")));
    }

    @Test
    void legacyTagLinkSelectsTagTabAndFiltersResults() throws Exception {
        mvc.perform(get("/cafes").param("tag", "FACILITY:WIFI"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sortTags", true))
                .andExpect(model().attribute("hasTagFilter", true))
                .andExpect(model().attribute("cafes", hasSize(0)))
                .andExpect(model().attribute("urlClearTag", "/cafes?sort=tags"));
    }

    @Test
    @WithMockUser(username = "search-test@example.com", roles = "CAFEOWNER")
    void recommendationUsesMemberNeedsAndHidesTagFilters() throws Exception {
        mvc.perform(get("/cafes").param("sort", "recommend").param("tag", "FACILITY:WIFI"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sortRecommend", true))
                .andExpect(model().attribute("sortTags", false))
                .andExpect(model().attribute("hasTagFilter", false))
                .andExpect(model().attribute("noNeedsSet", true))
                .andExpect(content().string(not(containsString("id=\"tagFilters\""))));
    }

    @Test
    void multipleTagsRequireAllTagsAndExcludePendingCafes() throws Exception {
        Cafe both = cafeRepository.save(cafe("All tags", "Test", "02-111-1111", CafeStatus.APPROVED));
        Cafe partial = cafeRepository.save(cafe("Only wifi", "Test", "02-111-1112", CafeStatus.APPROVED));
        Cafe pending = cafeRepository.save(cafe("Pending tags", "Test", "02-111-1113", CafeStatus.PENDING));
        addFacility(both, FacilityTag.WIFI);
        addFacility(both, FacilityTag.PLUG);
        addFacility(partial, FacilityTag.WIFI);
        addFacility(pending, FacilityTag.WIFI);
        addFacility(pending, FacilityTag.PLUG);

        mvc.perform(get("/cafes").param("sort", "tags")
                        .param("tag", "FACILITY:WIFI", "FACILITY:PLUG", "FACILITY:WIFI"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cafes", hasSize(1)))
                .andExpect(content().string(containsString("All tags")))
                .andExpect(content().string(not(containsString("Only wifi"))))
                .andExpect(content().string(not(containsString("Pending tags"))))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        org.jsoup.Jsoup.parse(result.getResponse().getContentAsString())
                                .select("#tagFilters a").stream()
                                .anyMatch(a -> a.attr("href").equals("/cafes?sort=tags&tag=FACILITY%3APLUG"))));

        // 하나를 해제하면 남은 태그만 적용된다.
        mvc.perform(get("/cafes").param("sort", "tags").param("tag", "FACILITY:WIFI"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cafes", hasSize(2)));
        mvc.perform(get("/cafes").param("sort", "tags")
                        .param("tag", "FACILITY:WIFI", "FACILITY:PARKING"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cafes", hasSize(0)));
    }

    private void addFacility(Cafe cafe, FacilityTag tag) {
        CafeTag row = new CafeTag();
        row.setCafe(cafe);
        row.setFacilityTag(tag);
        cafeTagRepository.save(row);
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
