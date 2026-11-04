package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.CafeInfo;

/**
 * 영업정보 응답 DTO. 화면이 쓰는 필드만 내보내 엔티티에 컬럼이 늘어나도 응답이 따라 바뀌지 않게 한다.
 */
public record CafeInfoResponse(
        Long id,
        Long cafeId,
        String notice,
        String info,
        String openTime,
        String closeTime,
        String holiday
) {
    public static CafeInfoResponse from(CafeInfo ci) {
        return new CafeInfoResponse(
                ci.getId(),
                ci.getCafe() == null ? null : ci.getCafe().getId(),
                ci.getNotice(),
                ci.getInfo(),
                ci.getOpenTime(),
                ci.getCloseTime(),
                ci.getHoliday()
        );
    }
}
