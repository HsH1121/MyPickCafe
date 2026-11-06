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

    @NoArgsConstructor
    @Entity
    @Getter
    @Setter
    @AllArgsConstructor
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