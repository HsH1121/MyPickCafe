package com.example.MyPickCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PickBotResult {
    private Long   cafeId;
    private String cafeName;
    private String address;
    private String snippet;
    private Double score;
    private String mainPhoto;
}
