package com.example.MyPickCafe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "cafe_info")
public class CafeInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_info_id", nullable = false, unique = true)
    private Long id;

    // 응답에 카페 엔티티 전체(점주 회원 정보, 사업자 증빙 경로, 승인 상태)가 따라 나가지 않게 한다.
    // 저장할 때는 컨트롤러가 경로의 cafeId 로 카페를 직접 넣으므로 요청 본문의 cafe 는 쓰지 않는다.
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cafe cafe;

    @Column(name = "notice", length = 100)
    private String notice;

    @Column(name = "info", length = 500)
    private String info;

    @Column(name = "open_time", length = 10)
    private String openTime;

    @Column(name = "close_time", length = 10)
    private String closeTime;

    @Column(name = "holiday", length = 20)
    private String holiday;

}