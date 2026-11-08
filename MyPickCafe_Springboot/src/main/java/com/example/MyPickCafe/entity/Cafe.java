package com.example.MyPickCafe.entity;

import com.example.MyPickCafe.domain.CafeStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
// 메인 페이지와 카페 목록 기본 정렬이 승인 카페를 조회수 내림차순으로 몇 개만 읽는다(ORDER BY views DESC LIMIT n).
// review.created_at 인덱스와 같은 이유로, 인덱스가 없으면 LIMIT 를 걸어도 승인 카페 전부를 정렬한다.
// status 단독은 값이 세 종류뿐이라 인덱스로 거르는 효과가 없어 views 와 묶는다.
// 조회수를 올리는 코드가 생기면 그때마다 이 인덱스도 갱신되니 쓰기 비용을 다시 따져 볼 것.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(
        name = "cafe",
        indexes = @Index(name = "idx_cafe_status_views", columnList = "status, views DESC"))
public class Cafe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member owner;

    @Column(name = "name", nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "address", nullable = false, length = 100)
    private String address;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String number;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime date;

    @Column(name = "views", nullable = false)
    private Long views = 0L;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private CafeStatus status = CafeStatus.PENDING;

    /**
     * 사업자 증빙 문서의 비공개 저장소 키(bizdocs/{cafeId}/{uuid}.pdf). 공개 URL 이 아니다.
     * 내용은 BizDocService 를 거쳐 관리자 엔드포인트로만 내려준다.
     * 엔티티가 실수로 JSON 응답에 실려도 키가 나가지 않게 직렬화에서 뺀다.
     */
    @JsonIgnore
    @Column(name = "biz_doc", length = 255)
    private String bizDoc;

    public boolean isApproved() {
        return status == CafeStatus.APPROVED;
    }

    public boolean isPending()  {
        return status == CafeStatus.PENDING;
    }

    public boolean isRejected() {
        return status == CafeStatus.REJECTED;
    }
}