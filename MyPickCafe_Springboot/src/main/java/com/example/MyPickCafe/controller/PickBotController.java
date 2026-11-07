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
        // 승인된 카페 ID 를 함께 넘겨, 픽봇이 그 카페의 리뷰만 벡터 검색(유사도 계산)하게 한다.
        // 검색 전에 거르므로 추천 결과를 다시 거르지 않는다.
        PickBotResponse response = pickBotClient.recommend(req.getQuery(), cafeService.findApprovedCafeIds());

        List<PickBotResult> results = response.getResults();
        if (!results.isEmpty()) {
            List<Long> cafeIds = results.stream().map(PickBotResult::getCafeId).toList();
            Map<Long, String> photoMap = cafePhotoService.findMainPhotoUrls(cafeIds);
            results.forEach(r -> r.setMainPhoto(photoMap.get(r.getCafeId())));
        }
        return ResponseEntity.ok(response);
    }
}
