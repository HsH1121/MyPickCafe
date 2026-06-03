package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.dto.ChatbotRequest;
import com.example.MyPickCafe.dto.ChatbotResult;
import com.example.MyPickCafe.service.ChatbotClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotClient chatbotClient;

    @PostMapping("/recommend")
    public ResponseEntity<List<ChatbotResult>> recommend(@RequestBody ChatbotRequest req) {
        if (req.getQuery() == null || req.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<ChatbotResult> results = chatbotClient.recommend(req.getQuery());
        return ResponseEntity.ok(results);
    }
}
