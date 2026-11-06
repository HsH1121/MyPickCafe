package com.example.MyPickCafe.api;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafeInfoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 영업정보(CafeInfo) 저장 API의 과다 바인딩(Mass Assignment) 회귀 테스트.
 *
 * <p>수정 전에는 요청 본문을 엔티티로 받아서 본문의 {@code id}가 그대로 바인딩됐다.
 * 영업정보가 없는 카페 A의 점주가 {@code POST /upsert/A}에 카페 B 영업정보의 id를 넣으면
 * {@code save()}가 merge 로 동작해 B의 행 내용을 덮어쓰고 cafe_id 까지 A로 옮겼다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("영업정보 저장 과다 바인딩 차단")
class CafeInfoMassAssignmentTest {

    private static final String ATTACKER = "info-attacker@example.com";
    private static final String VICTIM = "info-victim@example.com";

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CafeRepository cafeRepository;
    @Autowired private CafeInfoRepository cafeInfoRepository;

    private Long attackerCafeId;
    private Long victimCafeId;
    private Long victimInfoId;

    @BeforeEach
    void setUp() {
        attackerCafeId = cafe(member(ATTACKER, "attacker"), "공격자카페").getId();
        Cafe victimCafe = cafe(member(VICTIM, "victim"), "피해자카페");
        victimCafeId = victimCafe.getId();

        CafeInfo info = new CafeInfo();
        info.setCafe(victimCafe);
        info.setNotice("정상 영업");
        info.setInfo("피해자 카페 소개");
        info.setOpenTime("09:00");
        info.setCloseTime("21:00");
        info.setHoliday("없음");
        victimInfoId = cafeInfoRepository.save(info).getId();
        em.flush();
        em.clear();
    }

    private Member member(String email, String nickname) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(TestPasswords.HASH);
        m.setNickname(nickname);
        m.setRoleKind(RoleKind.CAFEOWNER);
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    private Cafe cafe(Member owner, String name) {
        Cafe c = new Cafe();
        c.setOwner(owner);
        c.setName(name);
        // 주소·전화번호는 unique 라 카페마다 다르게 둔다
        c.setAddress("서울시 마포구 " + name + "로 1");
        c.setNumber("02-777-" + Math.abs(name.hashCode() % 10000));
        c.setCode("CAFE");
        c.setDate(LocalDateTime.now());
        c.setViews(0L);
        c.setStatus(CafeStatus.APPROVED);
        return cafeRepository.save(c);
    }

    private static RequestPostProcessor attacker() { return user(ATTACKER).roles("CAFEOWNER"); }

    @Test
    @DisplayName("upsert 본문에 다른 카페 영업정보 id를 넣어도 그 행은 바뀌지 않는다")
    void upsertIgnoresIdInBody() throws Exception {
        String body = """
                {"id": %d, "notice": "폐업했습니다", "info": "공격자 소개",
                 "openTime": "00:00", "closeTime": "00:00", "holiday": "매일"}
                """.formatted(victimInfoId);

        mvc.perform(post("/api/cafe-infos/upsert/{cafeId}", attackerCafeId).with(attacker())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        em.flush();
        em.clear();

        CafeInfo victimInfo = cafeInfoRepository.findById(victimInfoId).orElseThrow();
        assertThat(victimInfo.getNotice()).isEqualTo("정상 영업");
        assertThat(victimInfo.getCafe().getId()).isEqualTo(victimCafeId);

        CafeInfo attackerInfo = cafeInfoRepository.findByCafe_Id(attackerCafeId).orElseThrow();
        assertThat(attackerInfo.getId()).isNotEqualTo(victimInfoId);
        assertThat(attackerInfo.getNotice()).isEqualTo("폐업했습니다");
    }

    @Test
    @DisplayName("upsert 는 자기 카페의 기존 영업정보를 수정한다")
    void upsertUpdatesOwnExistingRow() throws Exception {
        mvc.perform(post("/api/cafe-infos/upsert/{cafeId}", attackerCafeId).with(attacker())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notice\":\"첫 공지\"}"))
                .andExpect(status().isOk());
        em.flush();
        em.clear();
        Long ownId = cafeInfoRepository.findByCafe_Id(attackerCafeId).orElseThrow().getId();

        mvc.perform(post("/api/cafe-infos/upsert/{cafeId}", attackerCafeId).with(attacker())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notice\":\"바뀐 공지\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownId))
                .andExpect(jsonPath("$.notice").value("바뀐 공지"))
                .andExpect(jsonPath("$.cafe").doesNotExist());
        em.flush();
        em.clear();

        assertThat(cafeInfoRepository.findByCafe_Id(attackerCafeId).orElseThrow().getNotice())
                .isEqualTo("바뀐 공지");
    }

    @Test
    @DisplayName("다른 점주의 영업정보는 PUT 으로 수정할 수 없다")
    void putOnOthersInfoIsForbidden() throws Exception {
        mvc.perform(put("/api/cafe-infos/{id}", victimInfoId).with(attacker())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notice\":\"폐업했습니다\"}"))
                .andExpect(status().isForbidden());
        em.flush();
        em.clear();

        assertThat(cafeInfoRepository.findById(victimInfoId).orElseThrow().getNotice())
                .isEqualTo("정상 영업");
    }
}
