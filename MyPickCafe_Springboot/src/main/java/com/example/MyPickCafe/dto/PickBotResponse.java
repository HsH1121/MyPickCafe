package com.example.MyPickCafe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 픽봇 추천 응답.
 *
 * <p>{@code notice} 는 결과가 비었을 때 그 이유를 화면에 구분해 안내하기 위한 값이다.
 * 예: {@code REGION_NOT_FOUND} — 사용자가 말한 지역에 등록된 카페가 없음.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PickBotResponse {

    public static final String NOTICE_REGION_NOT_FOUND = "REGION_NOT_FOUND";

    private List<PickBotResult> results = new ArrayList<>();
    private String notice;
}
