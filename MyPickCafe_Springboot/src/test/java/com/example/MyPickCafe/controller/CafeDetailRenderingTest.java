package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafeInfoRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 카페 상세 페이지는 CafeInfo 의 영업 정보 일부가 null 이어도 렌더링돼야 한다.
 * (JMustache 는 {{변수}} 값이 null 이면 예외를 던져 500 이 났다)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("카페 상세 페이지 영업 정보 렌더링")
class CafeDetailRenderingTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired CafeRepository cafes;
    @Autowired CafeInfoRepository infos;
    private Cafe cafe;

    @BeforeEach
    void setUp() {
        Member owner = new Member();
        owner.setEmail("detail-test@example.com");
        owner.setPassword(TestPasswords.HASH);
        owner.setNickname("detail-test-owner");
        owner.setRoleKind(RoleKind.CAFEOWNER);
        members.save(owner);
        cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName("Detail test cafe");
        cafe.setAddress("Test address");
        cafe.setNumber("02-000-0000");
        cafe.setCode("CAFE");
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(0L);
        cafe.setStatus(CafeStatus.APPROVED);
        cafes.save(cafe);
    }

    @Test
    @DisplayName("오픈 시간만 있고 마감·휴무일이 null 이어도 200")
    void rendersWithNullBusinessHours() throws Exception {
        CafeInfo info = new CafeInfo();
        info.setCafe(cafe);
        info.setOpenTime("09:00");
        infos.save(info);

        mvc.perform(get("/cafes/{id}", cafe.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cafeCloseTime", ""))
                .andExpect(model().attribute("cafeHoliday", ""))
                .andExpect(content().string(containsString("<dt>오픈</dt><dd>09:00</dd>")))
                .andExpect(content().string(containsString("<dt>마감</dt><dd></dd>")));
    }

    @Test
    @DisplayName("영업 정보가 모두 null 이어도 200")
    void rendersWithAllInfoFieldsNull() throws Exception {
        CafeInfo info = new CafeInfo();
        info.setCafe(cafe);
        infos.save(info);

        mvc.perform(get("/cafes/{id}", cafe.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("소개/공지 정보가 아직 없어요.")));
    }
}
