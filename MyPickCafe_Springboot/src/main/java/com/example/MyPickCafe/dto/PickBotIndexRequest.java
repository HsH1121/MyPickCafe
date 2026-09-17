package com.example.MyPickCafe.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PickBotIndexRequest {
    private Long   reviewId;
    private Long   cafeId;
    private String cafeName;
    private String address;
    private String reviewText;
}
