package com.example.MyPickCafe.config;

import com.example.MyPickCafe.domain.*;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.*;
import com.example.MyPickCafe.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository             memberRepository;
    private final CafeRepository               cafeRepository;
    private final CafeInfoRepository           cafeInfoRepository;
    private final CategoryRepository           categoryRepository;
    private final MenuRepository               menuRepository;
    private final ReviewRepository             reviewRepository;
    private final TagDictionaryRepository      tagDictionaryRepository;
    private final RagReviewExampleRepository   ragReviewExampleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0) return; // 이미 데이터 있으면 스킵

        /* ── 1. 회원 ── */
        Member admin  = save(member("admin@MyPickCafe.com",  "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW", "admin",   28, "M", RoleKind.ADMIN));
        Member u1     = save(member("member1@example.com",   "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW", "member1",   25, "M", RoleKind.MEMBER));
        Member u2     = save(member("member2@example.com",   "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW", "member2",   30, "F", RoleKind.MEMBER));
        Member u3     = save(member("member3@example.com",  "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW", "member3",   22, "M", RoleKind.MEMBER));
        Member u4     = save(member("member4@example.com",  "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW", "member4",  27, "F", RoleKind.MEMBER));
        Member u5     = save(member("member5@example.com",  "$2a$10$LfeiDObpfbKJOFzAIVH3ruGqdCpG2zy.yQAMWPQaZciCPTaM38uSW", "member5",   35, "M", RoleKind.MEMBER));

        /* ── 2. 카페 (홍대·상수·합정 일대) ── */
        Cafe c1 = saveCafe(u1, "블루버드",  "서울 마포구 홍익로 5길 20",         37.5568, 126.9241, "02-333-1001", "DESSERT");
        Cafe c2 = saveCafe(u2, "모닝브루",  "서울 마포구 와우산로 21",            37.5552, 126.9260, "02-333-1002", "CAFE");
        Cafe c3 = saveCafe(u3, "연트로",    "서울 마포구 연남동 226-30",          37.5548, 126.9221, "02-333-1003", "CAFE");
        Cafe c4 = saveCafe(u4, "하프문",    "서울 마포구 합정동 367-20",          37.5497, 126.9143, "02-333-1004", "BAR");
        Cafe c5 = saveCafe(u5, "상수다방",  "서울 마포구 상수동 318-5",           37.5479, 126.9248, "02-333-1005", "CAFE");
        Cafe c6 = saveCafe(u1, "오후세시",  "서울 마포구 홍익로 14길 11",         37.5576, 126.9237, "02-333-1006", "DESSERT");

        /* ── 3. CafeInfo ── */
        saveCafeInfo(c1, "신메뉴 출시!",
                "홍대 골목 안에 위치한 아늑한 디저트 카페입니다. 수제 케이크와 핸드드립 커피를 즐기실 수 있어요.",
                "10:00", "22:00", "월요일");
        saveCafeInfo(c2, "웰컴 드링크",
                "아침 일찍 문을 여는 브런치 카페입니다. 드립 백과 갓 구운 크루아상을 함께 즐겨보세요.",
                "08:00", "20:00", "없음");
        saveCafeInfo(c3, "테라스 오픈",
                "연남동 골목의 작은 카페. 야외 테라스에서 조용하게 책 읽기 좋은 공간입니다.",
                "11:00", "21:00", "화요일");
        saveCafeInfo(c4, "해피아워 5-7",
                "합정역 인근 무드 있는 바 카페. 낮에는 커피, 저녁에는 칵테일을 즐기실 수 있습니다.",
                "12:00", "24:00", "없음");
        saveCafeInfo(c5, "포토존 오픈",
                "상수동 오래된 골목에 위치한 레트로 다방. 쌍화차와 인절미 라떼가 시그니처입니다.",
                "10:30", "21:30", "수요일");
        saveCafeInfo(c6, "단체 예약 가능",
                "오후 세시, 노을이 들어오는 시간이 가장 아름다운 카페입니다. 수제 마들렌이 유명해요.",
                "09:00", "21:00", "없음");

        /* ── 4. 메뉴 카테고리 + 메뉴 ── */
        addMenus(c1, "음료",  new String[]{"핸드드립",   "아인슈페너", "라떼"},        new int[]{6500, 7000, 6000});
        addMenus(c1, "디저트",new String[]{"수제치즈케익", "마들렌"},                   new int[]{8500, 3000});

        addMenus(c2, "음료",  new String[]{"아메리카노",  "카푸치노",   "콜드브루"},     new int[]{4500, 5500, 5500});
        addMenus(c2, "푸드",  new String[]{"크루아상",    "에그타르트"},                new int[]{4000, 3500});

        addMenus(c3, "음료",  new String[]{"아메리카노",  "플랫화이트",  "유자에이드"},  new int[]{4500, 5500, 6000});
        addMenus(c3, "디저트",new String[]{"바스크치즈",  "티라미수"},                  new int[]{8000, 7500});

        addMenus(c4, "커피",  new String[]{"에스프레소",  "더블샷라떼"},                new int[]{4000, 6000});
        addMenus(c4, "칵테일",new String[]{"에스프레소마티", "위스키사워"},             new int[]{13000, 14000});

        addMenus(c5, "전통차",new String[]{"쌍화차",      "대추차",     "생강차"},       new int[]{5000, 4500, 4500});
        addMenus(c5, "음료",  new String[]{"인절미라떼",  "흑임자라떼"},                new int[]{6500, 6500});

        addMenus(c6, "음료",  new String[]{"아메리카노",  "로즈라떼",   "자몽티"},       new int[]{4500, 6500, 5500});
        addMenus(c6, "베이커리",new String[]{"수제마들렌", "레몬파운드"},               new int[]{2500, 4000});

        /* ── 5. 리뷰 ── */
        saveReview(c1, u2, "케이크가 정말 맛있어요! 아인슈페너도 진해서 좋았습니다. 주말엔 웨이팅이 있지만 충분히 기다릴 만해요.", "GOOD");
        saveReview(c1, u3, "수제 케이크 종류가 다양하고 플레이팅도 예뻐서 사진 찍기 딱이에요. 가격이 조금 비싸긴 하지만 분위기값 합니다.", "GOOD");
        saveReview(c1, u4, "홍대에서 이런 조용한 카페 찾기 쉽지 않아요. 핸드드립 퀄리티가 높아서 자주 올 것 같아요.", "GOOD");

        saveReview(c2, u1, "아침 8시부터 열어서 출근 전에 들르기 좋아요. 크루아상이 갓 구워져서 진짜 맛있었어요.", "GOOD");
        saveReview(c2, u3, "콜드브루가 깔끔하고 산미가 좋았어요. 인테리어도 심플하고 작업하기 좋은 환경이에요.", "GOOD");
        saveReview(c2, u5, "브런치 메뉴 좀 더 다양했으면 좋겠어요. 에그타르트는 맛있는데 종류가 적어서 아쉬워요.", "BAD");

        saveReview(c3, u1, "연남동 감성 제대로입니다. 테라스에서 마신 유자에이드 최고였어요.", "GOOD");
        saveReview(c3, u4, "바스크 치즈케이크가 촉촉하고 달지 않아서 정말 좋았어요. 재방문 의사 100%!", "GOOD");
        saveReview(c3, u5, "주차공간이 없어서 불편했지만 카페 자체는 너무 예뻐요. 플랫화이트가 진하고 고소해요.", "GOOD");

        saveReview(c4, u2, "에스프레소 마티니가 이 가격에 이 퀄리티면 진짜 대박이에요. 바 분위기도 너무 좋고요.", "GOOD");
        saveReview(c4, u3, "낮에 커피 마시러 갔다가 저녁에 칵테일까지 마셨어요. 공간이 넓고 조용해서 데이트하기 딱이에요.", "GOOD");

        saveReview(c5, u1, "인절미 라떼가 진짜 전통 맛이에요. 레트로 인테리어도 너무 좋고 사진 찍기 좋아요.", "GOOD");
        saveReview(c5, u2, "쌍화차가 따뜻하고 향이 좋아요. 혼자 조용히 책 읽기 완벽한 공간이에요.", "GOOD");
        saveReview(c5, u4, "흑임자 라떼도 고소하고 맛있는데 양이 좀 적은 느낌이에요. 분위기는 100점!", "GOOD");

        saveReview(c6, u3, "오후에 창문으로 노을이 들어올 때 정말 아름다웠어요. 마들렌이 갓 구워져서 따뜻하고 버터향 가득!", "GOOD");
        saveReview(c6, u5, "로즈라떼가 향이 강하지 않고 부드러워서 좋았어요. 오후에 가면 자리 잡기 힘들 수 있으니 평일 추천해요.", "GOOD");

        /* ── 6. RAG 태그 설명 ── */
        initRagTagDescriptions();

        /* ── 7. RAG 리뷰 예시 ── */
        initRagReviewExamples();
    }

    private void initRagTagDescriptions() {
        for (FacilityTag tag : FacilityTag.values()) saveTagDesc(tag);
        for (MenuTag     tag : MenuTag.values())     saveTagDesc(tag);
        for (PurposeTag  tag : PurposeTag.values())  saveTagDesc(tag);
        for (MoodTag     tag : MoodTag.values())     saveTagDesc(tag);
    }

    private void saveTagDesc(TagEnum tag) {
        tagDictionaryRepository.save(
            TagDictionary.builder()
                .tagCategory(tag.getCategory())
                .tagEnum(tag.name())
                .tagLabel(tag.getLabel())
                .description(tag.getDescription())
                .keywords(tag.getKeywords())
                .build()
        );
    }

    private void initRagReviewExamples() {
        saveEx("콘센트가 많아서 노트북 작업하기 정말 좋아요. 와이파이도 빠르고 안정적이에요.",
               "PLUG,WIFI", null, "STUDY", "MODERN");
        saveEx("야외 테라스가 있어서 강아지 데리고 갈 수 있어요. 반려동물 환영 카페입니다.",
               "TERRACE,PET", null, "REST", "NATURE");
        saveEx("주차 공간이 넉넉해서 차 끌고 가기 편해요. 자리도 많고 소파석이 편안했어요.",
               "PARKING,CHAIR", null, "REST", null);
        saveEx("아메리카노가 정말 맛있어요. 산미가 적당하고 깔끔한 맛이에요.",
               null, "AMERICANO", null, "MODERN");
        saveEx("라떼 종류가 다양하고 크리미해서 좋았어요. 흑임자 라떼가 특히 고소했어요.",
               null, "LATTE", null, "RETRO");
        saveEx("콜드브루가 진하고 부드러워요. 질소 콜드브루도 있어서 선택지가 넓어요.",
               null, "COLDBREW", null, "MODERN");
        saveEx("크루아상이 갓 구워져서 버터향이 가득해요. 마들렌도 촉촉하고 맛있어요.",
               null, "BAKERY", "REST", null);
        saveEx("치즈케이크 맛집이에요! 바스크 치즈케이크가 촉촉하고 달지 않아서 좋았어요.",
               null, "CAKE", "DATE", "CLASSIC");
        saveEx("유자에이드가 상큼하고 맛있어요. 시그니처 에이드가 다양해서 고르는 재미가 있어요.",
               null, "ADE", "DATE", "MODERN");
        saveEx("디저트 종류가 정말 많아요. 티라미수랑 마카롱이 특히 맛있었어요.",
               null, "DESSERT,CAKE", "TALK", "CLASSIC");
        saveEx("조용한 편이라 혼자 공부하러 자주 와요. 콘센트도 많고 집중하기 좋아요.",
               "PLUG,WIFI", "AMERICANO", "STUDY", "MODERN");
        saveEx("친구들이랑 수다 떨기 좋아요. 테이블이 넓고 오래 앉아 있어도 눈치 안 줘요.",
               "CHAIR", null, "TALK", "RETRO");
        saveEx("연인이랑 데이트하기 딱 좋은 분위기예요. 조명이 따뜻하고 아늑해요.",
               null, "LATTE,DESSERT", "DATE", "CLASSIC");
        saveEx("사진 찍기 너무 좋아요! 포토존도 있고 인테리어 하나하나가 다 예뻐요.",
               null, "CAKE", "PHOTO", "RETRO");
        saveEx("루프탑 테라스에서 보는 뷰가 최고예요. 야외에서 커피 마시는 느낌이 좋아요.",
               "TERRACE", "AMERICANO", "REST,DATE", "NATURE");
        saveEx("비즈니스 미팅하기 좋아요. 조용하고 와이파이도 잘 터지고 좌석도 편안해요.",
               "WIFI,CHAIR", "AMERICANO", "MEETING", "MODERN");
        saveEx("레트로 감성 가득한 다방이에요. 쌍화차랑 전통 간식이 맛있어요.",
               null, "DESSERT", "REST,PHOTO", "RETRO");
        saveEx("식물이 많아서 힐링되는 느낌이에요. 자연스러운 인테리어가 눈을 편하게 해줘요.",
               null, null, "REST", "NATURE");
        saveEx("벽돌 인테리어가 멋진 인더스트리얼 카페예요. 에스프레소가 진하고 향이 좋아요.",
               null, "AMERICANO", "PHOTO", "INDUSTRIAL");
        saveEx("단체 모임하기 좋아요. 큰 테이블이 있고 주차도 가능해서 접근하기 편해요.",
               "PARKING,CHAIR", null, "MEETING,TALK", null);
    }

    private void saveEx(String text, String facility, String menu, String purpose, String mood) {
        ragReviewExampleRepository.save(
            RagReviewExample.builder()
                .reviewText(text)
                .facilityTags(facility)
                .menuTags(menu)
                .purposeTags(purpose)
                .moodTags(mood)
                .build()
        );
    }

    /* ── helpers ── */

    private Member member(String email, String password, String nickname, int age, String gender, RoleKind role) {
        Member m = new Member();
        m.setEmail(email);
        m.setPassword(password);
        m.setNickname(nickname);
        m.setAge((long) age);
        m.setGender(gender);
        m.setRoleKind(role);
        m.setTokenVersion(0L);
        return m;
    }

    private Member save(Member m) {
        return memberRepository.save(m);
    }

    private Cafe saveCafe(Member owner, String name, String address,
                          double lat, double lon, String number, String code) {
        Cafe c = new Cafe();
        c.setOwner(owner);
        c.setName(name);
        c.setAddress(address);
        c.setLat(lat);
        c.setLon(lon);
        c.setNumber(number);
        c.setCode(code);
        c.setDate(LocalDateTime.now().minusMonths((long)(Math.random() * 24 + 1)));
        c.setViews((long)(Math.random() * 800 + 50));
        c.setStatus(CafeStatus.APPROVED);
        return cafeRepository.save(c);
    }

    private void saveCafeInfo(Cafe cafe, String notice, String info,
                               String open, String close, String holiday) {
        CafeInfo ci = new CafeInfo();
        ci.setCafe(cafe);
        ci.setNotice(notice);
        ci.setInfo(info);
        ci.setOpenTime(open);
        ci.setCloseTime(close);
        ci.setHoliday(holiday);
        cafeInfoRepository.save(ci);
    }

    private void addMenus(Cafe cafe, String categoryName, String[] names, int[] prices) {
        MenuCategory cat = new MenuCategory();
        cat.setCafe(cafe);
        cat.setCategory(categoryName);
        categoryRepository.save(cat);

        for (int i = 0; i < names.length; i++) {
            Menu m = new Menu();
            m.setCafe(cafe);
            m.setMenuCategory(cat);
            m.setName(names[i]);
            m.setPrice(prices[i]);
            m.setNew(i == 0);
            m.setRecommended(i == 0);
            menuRepository.save(m);
        }
    }

    private void saveReview(Cafe cafe, Member member, String content, String sentiment) {
        Review r = Review.builder()
                .cafe(cafe)
                .member(member)
                .content(content)
                .sentiment(sentiment)
                .good("GOOD".equals(sentiment) ? 1 : 0)
                .bad("BAD".equals(sentiment)  ? 1 : 0)
                .createdAt(LocalDateTime.now().minusDays((long)(Math.random() * 60 + 1)))
                .build();
        reviewRepository.save(r);
    }
}
