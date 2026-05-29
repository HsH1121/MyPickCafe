package com.example.MyPickCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CafeCardForm {
    private final Long id;
    private final String name;
    private final String address;
    private final String number;
    private final String code;
    private final Long views;
    private final String mainPhoto;
}
