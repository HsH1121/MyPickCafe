package com.example.MyPickCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatbotResult {
    private Long   cafeId;
    private String cafeName;
    private String address;
    private String snippet;   // 관련 리뷰 발췌문
    private Double score;     // 유사도 점수
}
