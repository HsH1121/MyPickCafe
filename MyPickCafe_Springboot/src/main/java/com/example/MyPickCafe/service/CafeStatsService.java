package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.TagEnum;
import com.example.MyPickCafe.entity.CafeTag;
import com.example.MyPickCafe.repository.CafeTagRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CafeStatsService {

    private static final List<String> CATEGORY_ORDER = List.of("FACILITY", "MENU", "PURPOSE", "MOOD");

    private final ReviewRepository reviewRepository;
    private final CafeTagRepository cafeTagRepository;

    public Map<String, Object> buildStats(Long cafeId, int topN) {
        int good = reviewRepository.countByCafe_IdAndSentiment(cafeId, "GOOD");
        int bad  = reviewRepository.countByCafe_IdAndSentiment(cafeId, "BAD");

        // 대표 태그(cafe_tag) — 리뷰 태그 집계로 선정된 결과를 카테고리 순서대로 노출
        List<TagEnum> picked = new ArrayList<>();
        for (CafeTag ct : cafeTagRepository.findByCafe_Id(cafeId)) {
            TagEnum t = ct.getFacilityTag() != null ? ct.getFacilityTag()
                      : ct.getMenuTag()     != null ? ct.getMenuTag()
                      : ct.getPurposeTag()  != null ? ct.getPurposeTag()
                      : ct.getMoodTag();
            if (t != null) picked.add(t);
        }
        picked.sort(Comparator.comparingInt(t -> CATEGORY_ORDER.indexOf(t.getCategory())));

        List<Map<String, Object>> tags = new ArrayList<>();
        for (TagEnum t : picked) {
            if (tags.size() >= topN) break;
            tags.add(Map.of("categoryCode", t.getCategory(), "code", t.name(), "label", t.getLabel()));
        }
        return Map.of("good", good, "bad", bad, "tags", tags);
    }

}
