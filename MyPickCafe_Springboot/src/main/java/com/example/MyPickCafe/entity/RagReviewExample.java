package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import jakarta.persistence.Lob;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rag_review_example")
public class RagReviewExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "review_text", nullable = false, length = 1000)
    private String reviewText;

    @Column(name = "facility_tags", length = 100)
    private String facilityTags;  // comma-separated enums, e.g. "WIFI,PLUG"

    @Column(name = "menu_tags", length = 100)
    private String menuTags;

    @Column(name = "purpose_tags", length = 100)
    private String purposeTags;

    @Column(name = "mood_tags", length = 100)
    private String moodTags;

    @Lob
    @Column(name = "embedding")
    private String embedding;     // JSON float array, populated by FastAPI

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
