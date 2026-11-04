package com.example.MyPickCafe.dto;

import jakarta.validation.constraints.Size;

/**
 * 영업정보 저장 요청 DTO. 전달한 값으로 영업정보 전체를 교체한다(null 은 비움).
 *
 * <p>id·소속 카페는 받지 않는다. 엔티티를 그대로 받던 기존 방식에서는 본문의 id 가
 * 바인딩되어, 다른 카페의 영업정보 행을 덮어쓰고 자기 카페로 옮길 수 있었다.
 */
public record CafeInfoRequest(
        @Size(max = 100) String notice,
        @Size(max = 500) String info,
        @Size(max = 10) String openTime,
        @Size(max = 10) String closeTime,
        @Size(max = 20) String holiday
) {
}
