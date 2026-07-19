package com.example.MyPickCafe.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * 카페 수정 요청 DTO. null 필드는 "변경하지 않음"을 의미한다.
 *
 * <p>승인 상태 변경은 관리자 전용 엔드포인트
 * ({@code POST /admin/cafes/{id}/approve|reject})로만 가능하다.
 */
public record CafeUpdateRequest(
        @Size(max = 30)
        String name,

        @Size(max = 100)
        String address,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        Double lat,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        Double lon,

        @Size(max = 15)
        String phone,

        @Size(max = 10)
        String code
) {
}
