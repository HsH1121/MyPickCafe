package com.example.MyPickCafe.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class TagChipDto {

    private final String categoryCode;
    private final String code;
}
