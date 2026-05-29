package com.example.MyPickCafe.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

@Controller
public class MapController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // application.properties → kakao.js-key=${KAKAO_JS_KEY}
    @Value("${kakao.js-key}")
    private String kakaoJsKey;


    @GetMapping("/index/map")
    public String mapPage(Model model) throws JsonProcessingException {
        // 앵커(홍대입구 · 상수 · 연트럴 · 합정)
        List<Anchor> anchors = getAnchors();

        // 예시 장소 데이터(실서비스 시 DB/Service 연동)
        List<Place> places = samplePlaces();

        // 뷰에 전달
        model.addAttribute("isMap",true);
        model.addAttribute("kakaoJsKey", kakaoJsKey);
        model.addAttribute("places", places); // 사이드 리스트
        model.addAttribute("placesJson", objectMapper.writeValueAsString(places));   // 지도/모달
        model.addAttribute("anchorsJson", objectMapper.writeValueAsString(anchors)); // bounds 계산

        return "page/map"; // -> src/main/resources/templates/page/map.mustache
    }

    private List<Anchor> getAnchors() {
        return Arrays.asList(
                Anchor.builder().id("hongdae").name("홍대입구역").lat(37.5540).lng(126.9208).build(),
                Anchor.builder().id("sangsu").name("상수역").lat(37.54761).lng(126.92127).build(),
                Anchor.builder().id("yeontral").name("연트럴파크(경의선숲길)").lat(37.5593).lng(126.9248).build(),
                Anchor.builder().id("hapjeong").name("합정역").lat(37.5428).lng(126.9082).build()
        );
    }

    // 샘플 데이터(원하면 제거하고 DB 연동)
    private List<Place> samplePlaces() {
        return Arrays.asList(
                Place.builder()
                        .id(101L)
                        .name("카페 소셜클럽")
                        .rating(4.6)
                        .address("서울 마포구 와우산로 00")
                        .lat(37.5532)
                        .lng(126.9219)
                        .tags(Arrays.asList("라떼 맛집", "브런치", "감성"))
                        .thumbnail(null)
                        .build(),
                Place.builder()
                        .id(102L)
                        .name("브루잉랩")
                        .rating(4.4)
                        .address("서울 마포구 독막로 00")
                        .lat(37.5489)
                        .lng(126.9197)
                        .tags(Arrays.asList("핸드드립", "로스터리"))
                        .thumbnail(null)
                        .build()
        );
    }

    // ---- DTOs ----
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Anchor {
        private String id;
        private String name;
        private double lat;
        private double lng;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Place {
        private Long id;
        private String name;
        private Double rating;
        private String address;
        private Double lat;
        private Double lng;
        private List<String> tags;
        private String thumbnail;
    }
}
