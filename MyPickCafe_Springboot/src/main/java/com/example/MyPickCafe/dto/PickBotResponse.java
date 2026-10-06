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
 * <p>{@code notice} 는 화면에 따로 안내할 내용이 있을 때의 값이다.
 * <ul>
 *   <li>{@code REGION_NOT_FOUND} — 사용자가 말한 지역에 등록된 카페가 없음 (results 비어 있음).</li>
 *   <li>{@code OUTSIDE_SEOUL} — 서울 밖 지명을 말함. 그 지역은 거르지 않고 찾은 결과와 함께
 *       "서울 내 카페만 검색할 수 있어요"를 안내한다.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PickBotResponse {

    public static final String NOTICE_REGION_NOT_FOUND = "REGION_NOT_FOUND";
    public static final String NOTICE_OUTSIDE_SEOUL = "OUTSIDE_SEOUL";

    private List<PickBotResult> results = new ArrayList<>();
    private String notice;
}
