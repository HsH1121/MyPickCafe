package com.example.MyPickCafe.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatbotIndexRequest {
    private Long   reviewId;
    private Long   cafeId;
    private String cafeName;
    private String address;
    private String reviewText;
}
