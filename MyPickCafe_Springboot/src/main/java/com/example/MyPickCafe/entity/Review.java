package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
// PostgreSQL 은 FK 컬럼에 인덱스를 자동으로 만들지 않는다. 리뷰는 수만 건이라 인덱스가 없으면 아래 조회가 모두 전체 스캔이다.
// - created_at: 메인·검색 페이지가 최근 리뷰를 created_at 내림차순으로 6건만 읽는다.
//   인덱스가 없으면 전 행을 top-N 정렬해야 해서 LIMIT 를 걸어도 느리다.
// - (cafe_id, created_at): 카페 상세의 리뷰 목록(최신순)과 GOOD/BAD 집계, 리뷰 작성 시 태그 집계, 픽봇 대표 리뷰.
//   상세 페이지 한 번에 세 번 돈다. cafe_id 만 거는 조회도 선두 컬럼이라 이 인덱스를 쓴다.
// - (member_id, created_at): 마이페이지 "내 리뷰" 페이징(최신순)과 그 count 쿼리.
// 두 FK 인덱스는 카페·회원 삭제 시 ON DELETE CASCADE 가 review 를 전체 스캔하는 것도 막는다.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(
        name = "review",
        indexes = {
                @Index(name = "idx_review_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_review_cafe_id_created_at", columnList = "cafe_id, created_at DESC"),
                @Index(name = "idx_review_member_id_created_at", columnList = "member_id, created_at DESC")
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cafe cafe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(name = "content", nullable = false, length = 1500)
    private String content;

    @Builder.Default
    @Column(name = "good", nullable = false)
    private int good = 0;

    @Builder.Default
    @Column(name = "bad", nullable = false)
    private int bad = 0;

    @Column(name = "sentiment")
    private String sentiment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @Transient
    public String getMemberNickname() {
        return (member != null ? member.getNickname() : null);
    }

    @Transient
    public String getCreatedAtFmt() {
        return createdAt != null
                ? createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                : "";
    }
}
