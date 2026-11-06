package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CafeForm {

    private Member owner;

    // 기본 정보
    private String name;
    private String address;
    private Double lat;
    private Double lon;
    private String number;
    private Long views;
    private String code;
    // 사업자 증빙은 폼 값으로 받지 않는다. 업로드 파일(bizDocFile)만 CafeService 가 비공개 저장소에 넣고 키를 정한다.
    // (예전에는 bizDoc 문자열이 그대로 바인딩돼 외부 URL·서버 경로를 넣을 수 있었다)

    // 카페 소개 (CafeInfo)
    private String openTime;
    private String closeTime;
    private String holiday;
    private String notice;
    private String info;

    public Cafe toEntity() {
        Cafe cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName(name);
        cafe.setAddress(address);
        cafe.setLat(lat);
        cafe.setLon(lon);
        cafe.setNumber(number);
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(views != null ? views : 0L);
        cafe.setCode(code);
        cafe.setStatus(CafeStatus.PENDING);
        return cafe;
    }
}
