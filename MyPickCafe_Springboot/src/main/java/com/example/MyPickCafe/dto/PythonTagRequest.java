package com.example.MyPickCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PythonTagRequest {
    private Long   reviewId;
    private String reviewText;
}
