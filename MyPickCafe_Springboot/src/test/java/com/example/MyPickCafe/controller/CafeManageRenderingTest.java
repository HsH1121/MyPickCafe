package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafeInfoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "manage-test@example.com", roles = "CAFEOWNER")
class CafeManageRenderingTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired CafeRepository cafes;
    @Autowired CafeInfoRepository infos;
    private Cafe cafe;

    @BeforeEach
    void setUp() {
        Member owner = new Member();
        owner.setEmail("manage-test@example.com");
        owner.setPassword("unused-in-mock-authentication");
        owner.setNickname("manage-test-owner");
        owner.setRoleKind(RoleKind.CAFEOWNER);
        members.save(owner);
        cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName("Manage test cafe");
        cafe.setAddress("Test address");
        cafe.setNumber("02-000-0000");
        cafe.setCode("CAFE");
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(0L);
        cafe.setStatus(CafeStatus.APPROVED);
        cafes.save(cafe);
    }

    @Test
    void rendersEmptyFieldsWithoutCafeInfo() throws Exception {
        mvc.perform(get("/cafes/{id}/manage", cafe.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("infoOpenTime", ""))
                .andExpect(model().attribute("infoCloseTime", ""))
                .andExpect(model().attribute("infoHoliday", ""))
                .andExpect(model().attribute("infoNotice", ""))
                .andExpect(model().attribute("infoInfo", ""))
                .andExpect(content().string(containsString("id=\"infoOpenTime\" type=\"time\" value=\"\"")));
    }

    @Test
    void preservesExistingInfoAndRendersNullFieldsAsEmpty() throws Exception {
        CafeInfo info = new CafeInfo();
        info.setCafe(cafe);
        info.setOpenTime("09:00");
        info.setNotice("Welcome");
        infos.save(info);
        mvc.perform(get("/cafes/{id}/manage", cafe.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("infoOpenTime", "09:00"))
                .andExpect(model().attribute("infoNotice", "Welcome"))
                .andExpect(model().attribute("infoCloseTime", ""))
                .andExpect(model().attribute("infoHoliday", ""))
                .andExpect(model().attribute("infoInfo", ""))
                .andExpect(content().string(containsString("value=\"09:00\"")));
    }
}
