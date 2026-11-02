package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.dto.PickBotRequest;
import com.example.MyPickCafe.dto.PickBotResponse;
import com.example.MyPickCafe.dto.PickBotResult;
import com.example.MyPickCafe.service.CafePhotoService;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.PickBotClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/pickbot")
@RequiredArgsConstructor
public class PickBotController {

    private final PickBotClient pickBotClient;
    private final CafePhotoService cafePhotoService;
    private final CafeService cafeService;

    @PostMapping("/recommend")
    public ResponseEntity<PickBotResponse> recommend(@RequestBody PickBotRequest req) {
        if (req.getQuery() == null || req.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        PickBotResponse response = pickBotClient.recommend(req.getQuery());
        // AI 서버는 승인된 카페만 추천하도록 걸러 두었지만(카페 목록 캐시·벡터 검색 조건), 캐시가 최대 5분
        // 늦을 수 있고 인덱스에는 승인 취소된 카페 리뷰가 남아 있을 수 있다. 응답 직전에 DB 기준으로 한 번 더 거른다.
        Set<Long> approved = cafeService.filterApprovedIds(
                response.getResults().stream().map(PickBotResult::getCafeId).toList());
        response.getResults().removeIf(r -> r.getCafeId() == null || !approved.contains(r.getCafeId()));
        List<PickBotResult> results = response.getResults();
        if (!results.isEmpty()) {
            List<Long> cafeIds = results.stream().map(PickBotResult::getCafeId).toList();
            Map<Long, String> photoMap = cafePhotoService.findMainPhotoUrls(cafeIds);
            results.forEach(r -> r.setMainPhoto(photoMap.get(r.getCafeId())));
        }
        return ResponseEntity.ok(response);
    }
}
