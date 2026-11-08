package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
// (member_id, cafe_id) 유니크 인덱스는 선두가 member_id 라서 cafe_id 만으로 거는 조회에는 못 쓴다.
// 카페 상세를 열 때마다 도는 찜 수(countByCafe_Id)와 카페 삭제 시 CASCADE 를 위해 cafe_id 인덱스를 따로 둔다.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(
        name = "favorite",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "cafe_id"}),
        indexes = @Index(name = "idx_favorite_cafe_id", columnList = "cafe_id")
)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cafe cafe;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}