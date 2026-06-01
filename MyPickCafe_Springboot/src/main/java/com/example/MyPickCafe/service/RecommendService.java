package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.CafeCardForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.entity.UserNeeds;
import com.example.MyPickCafe.repository.CafePhotoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.CafeTagRepository;
import com.example.MyPickCafe.repository.NeedsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final NeedsRepository needsRepository;
    private final CafeTagRepository cafeTagRepository;
    private final CafeRepository cafeRepository;
    private final CafePhotoRepository cafePhotoRepository;

    private static final String PLACEHOLDER = "/images/placeholder-cafe.jpg";

    @Transactional(readOnly = true)
    public List<CafeCardForm> recommendForMember(Long memberId, int limit) {
        List<UserNeeds> needs = needsRepository.findByMember_Id(memberId);
        if (needs.isEmpty()) return List.of();

        Set<String> needsSet = needs.stream()
                .map(n -> n.getCategoryCode() + ":" + n.getCode())
                .collect(Collectors.toSet());

        // cafeId -> 해당 카페의 태그 집합 (엔티티 전체 로드 없이 태그 문자열만 조회)
        Map<Long, Set<String>> tagsByCafe = new HashMap<>();
        for (Object[] row : cafeTagRepository.findAllCafeTagStrings()) {
            Long cafeId = ((Number) row[0]).longValue();
            String tagStr = (String) row[1];
            if (tagStr != null) {
                tagsByCafe.computeIfAbsent(cafeId, k -> new HashSet<>()).add(tagStr);
            }
        }

        // 자카드 유사도 계산 후 내림차순 정렬
        List<Long> topCafeIds = tagsByCafe.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), jaccard(needsSet, e.getValue())))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topCafeIds.isEmpty()) return List.of();

        // 카페 정보 조회
        Map<Long, Cafe> cafeMap = cafeRepository.findAllById(topCafeIds).stream()
                .collect(Collectors.toMap(Cafe::getId, c -> c));

        // 대표 사진 조회
        Map<Long, String> photoMap = new HashMap<>();
        for (CafePhoto p : cafePhotoRepository.findForCafeIdsOrderByMainThenSort(topCafeIds)) {
            photoMap.putIfAbsent(p.getCafe().getId(), p.getUrl());
        }

        return topCafeIds.stream()
                .map(cafeMap::get)
                .filter(Objects::nonNull)
                .map(c -> new CafeCardForm(
                        c.getId(), c.getName(), c.getAddress(),
                        c.getNumber(), c.getCode(), c.getViews(),
                        photoMap.getOrDefault(c.getId(), PLACEHOLDER)))
                .collect(Collectors.toList());
    }

    private double jaccard(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}
