package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "review")
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
