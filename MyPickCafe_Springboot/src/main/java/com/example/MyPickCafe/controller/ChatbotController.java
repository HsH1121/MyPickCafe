package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.dto.ChatbotRequest;
import com.example.MyPickCafe.dto.ChatbotResult;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.service.CafePhotoService;
import com.example.MyPickCafe.service.ChatbotClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotClient chatbotClient;
    private final CafePhotoService cafePhotoService;

    @PostMapping("/recommend")
    public ResponseEntity<List<ChatbotResult>> recommend(@RequestBody ChatbotRequest req) {
        if (req.getQuery() == null || req.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<ChatbotResult> results = chatbotClient.recommend(req.getQuery());
        if (!results.isEmpty()) {
            List<Long> cafeIds = results.stream().map(ChatbotResult::getCafeId).toList();
            Map<Long, String> photoMap = cafePhotoService.findForCafeIdsOrderByMainThenSort(cafeIds)
                    .stream()
                    .collect(Collectors.toMap(
                            p -> p.getCafe().getId(),
                            CafePhoto::getUrl,
                            (first, second) -> first
                    ));
            results.forEach(r -> r.setMainPhoto(photoMap.get(r.getCafeId())));
        }
        return ResponseEntity.ok(results);
    }
}
