package com.example.MyPickCafe.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PythonTagRequest {
    private Long   reviewId;
    private String reviewText;
}
