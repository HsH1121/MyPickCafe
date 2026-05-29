package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

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

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Builder.Default
    @Column(name = "good", nullable = false)
    private int good = 0;

    @Builder.Default
    @Column(name = "bad", nullable = false)
    private int bad = 0;

    @Column(name = "waiting_time")
    private Integer waitingTime;

    @Column(name = "companion_type")
    private String companionType;

    @Column(name = "taste")
    private Integer taste;

    @Column(name = "sentiment")
    private String sentiment;

    @Column(name = "liked_tag_csv")
    private String likedTagCsv;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY)
    private List<ReviewTag> tags = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "review", cascade=ALL, orphanRemoval=true)
    @OrderBy("sortIndex ASC")
    private List<ReviewPhoto> photos = new ArrayList<>();

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
    private void applySentimentCountsOnCreate() {
        String s = normalizeSentiment(this.sentiment);

        // 신규 생성 시 good/bad가 아직 0이면 라디오 선택을 숫자 칼럼에 1로 반영
        if (this.good == 0 && this.bad == 0) {
            if ("GOOD".equals(s)) {
                this.good = 1;
                this.bad = 0;
            } else if ("BAD".equals(s)) {
                this.good = 0;
                this.bad = 1;
            }
        }

        // 저장되는 sentiment는 GOOD/BAD만 허용, 그 외는 null
        this.sentiment = ("GOOD".equals(s) || "BAD".equals(s)) ? s : null;
    }
    private String normalizeSentiment(String raw) {
        return raw == null ? null : raw.trim().toUpperCase();
    }
}