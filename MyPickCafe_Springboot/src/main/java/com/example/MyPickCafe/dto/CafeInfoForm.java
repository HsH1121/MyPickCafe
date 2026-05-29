package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CafeInfoForm {

    private Long cafeInfoId;
    private Cafe cafe;
    private String cafeNotice;
    private String cafeInfo;
    private String cafeOpenTime;
    private String cafeCloseTime;
    private String cafeHoliday;

    public CafeInfo toEntity() {
        return new CafeInfo(
                cafeInfoId,
                cafe,
                cafeNotice,
                cafeInfo,
                cafeOpenTime,
                cafeCloseTime,
                cafeHoliday
        );
    }
}
