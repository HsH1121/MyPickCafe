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
@Table(
        name = "review_photo",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_review_photo_order", columnNames = {"review_id", "sort_index"})
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ReviewPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_photo_id", nullable = false, unique = true)
    private Long id; // 기존: reviewPhotoId

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 반드시 리뷰에 속함
    @JoinColumn(name = "review_id", nullable = false)    // FK 컬럼
    @OnDelete(action = OnDeleteAction.CASCADE)           // 리뷰 삭제 시 사진도 삭제
    private Review review; // 기존: Long reviewId

    @Column(name = "photo_url", nullable = false, length = 1024) // 기존: review_photo_url
    private String url;

    @Column(name = "sort_index", nullable = false) // 기존: review_photo_sort_order
    private int sortIndex;
}