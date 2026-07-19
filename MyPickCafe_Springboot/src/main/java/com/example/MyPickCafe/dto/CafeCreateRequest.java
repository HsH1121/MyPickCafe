package com.example.MyPickCafe.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 카페 생성 요청 DTO.
 *
 * <p>{@code status}, {@code views}, {@code owner}는 서버가 결정한다.
 * 엔티티를 그대로 받던 기존 방식에서는 클라이언트가 이 값들을 직접 넣어
 * 승인 상태(APPROVED)나 조회수를 조작할 수 있었다.
 */
public record CafeCreateRequest(
        @NotBlank(message = "카페명은 필수입니다.")
        @Size(max = 30)
        String name,

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 100)
        String address,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        Double lat,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        Double lon,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Size(max = 15)
        String phone,

        @NotBlank(message = "카테고리는 필수입니다.")
        @Size(max = 10)
        String code
) {
}
