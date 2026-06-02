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
    private String bizDoc;

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
        cafe.setBizDoc(bizDoc);
        cafe.setStatus(CafeStatus.PENDING);
        return cafe;
    }
}
