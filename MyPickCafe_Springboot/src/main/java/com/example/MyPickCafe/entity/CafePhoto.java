package com.example.MyPickCafe.entity;

import com.example.MyPickCafe.config.StoredFileUrlConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
// 카드가 나오는 모든 화면이 카페별 대표 사진을, 카페 상세·관리가 사진 목록을 sort_index 순으로 읽는다.
@Table(name = "cafe_photo", indexes = @Index(name = "idx_cafe_photo_cafe_id_sort_index", columnList = "cafe_id, sort_index"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CafePhoto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_photo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    @JsonIgnore
    private Cafe cafe;

    @Convert(converter = StoredFileUrlConverter.class)
    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Column(name = "original_name", length = 120)
    private String originalName;

    @Column(name = "content_type", length = 60)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sort_index")
    private Integer sortIndex = 0;

    @Column(name = "is_main")
    private Boolean main = Boolean.FALSE;
}
