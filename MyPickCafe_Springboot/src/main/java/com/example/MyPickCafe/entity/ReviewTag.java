package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NoArgsConstructor
@Entity
@Table(
        name = "review_tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_tag_review_code", columnNames = {"review_id", "code"})
        }
)
@Getter @Setter
public class ReviewTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_tag_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Review review;

    // AI 추출 태그 카테고리 (FACILITY, MENU, PURPOSE, MOOD)
    @Column(name = "category_code", length = 20)
    private String categoryCode;

    @Column(name = "code", length = 20)
    private String code;


    public ReviewTag(Long id, Review review, String categoryCode, String code) {
        this.id = id;
        this.review = review;
        this.categoryCode = categoryCode;
        this.code = code;
    }
}