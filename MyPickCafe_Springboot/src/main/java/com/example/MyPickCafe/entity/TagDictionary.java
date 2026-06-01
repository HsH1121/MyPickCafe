package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import jakarta.persistence.Lob;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tag_dictionary")
public class TagDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tag_category", nullable = false, length = 20)
    private String tagCategory;   // FACILITY | MENU | PURPOSE | MOOD

    @Column(name = "tag_enum", nullable = false, length = 30)
    private String tagEnum;       // e.g. WIFI, AMERICANO

    @Column(name = "tag_label", nullable = false, length = 20)
    private String tagLabel;      // e.g. 와이파이

    @Lob
    @Column(name = "embedding")
    private String embedding;     // JSON float array, populated by FastAPI
}
