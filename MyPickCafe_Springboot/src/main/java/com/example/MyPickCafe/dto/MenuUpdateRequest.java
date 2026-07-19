package com.example.MyPickCafe.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 메뉴 수정 요청 DTO. null 필드는 "변경하지 않음"을 의미한다.
 *
 * <p>소속 카페({@code cafeId})는 변경할 수 없다. 엔티티를 그대로 받던
 * 기존 방식에서는 다른 카페의 ID를 넣어 메뉴를 가로챌 수 있었다.
 */
public record MenuUpdateRequest(
        @Size(max = 30)
        String name,

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Integer price,

        Boolean isNew,

        Boolean isRecommended
) {
}
