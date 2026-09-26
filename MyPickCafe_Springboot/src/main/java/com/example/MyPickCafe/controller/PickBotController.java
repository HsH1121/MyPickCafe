package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.dto.PickBotRequest;
import com.example.MyPickCafe.dto.PickBotResponse;
import com.example.MyPickCafe.dto.PickBotResult;
import com.example.MyPickCafe.service.CafePhotoService;
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

    @PostMapping("/recommend")
    public ResponseEntity<PickBotResponse> recommend(@RequestBody PickBotRequest req) {
        if (req.getQuery() == null || req.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        PickBotResponse response = pickBotClient.recommend(req.getQuery());
        List<PickBotResult> results = response.getResults();
        if (!results.isEmpty()) {
            List<Long> cafeIds = results.stream().map(PickBotResult::getCafeId).toList();
            Map<Long, String> photoMap = cafePhotoService.findMainPhotoUrls(cafeIds);
            results.forEach(r -> r.setMainPhoto(photoMap.get(r.getCafeId())));
        }
        return ResponseEntity.ok(response);
    }
}
