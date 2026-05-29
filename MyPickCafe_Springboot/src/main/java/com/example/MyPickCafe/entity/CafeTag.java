package com.example.MyPickCafe.entity;

import com.example.MyPickCafe.domain.FacilityTag;
import com.example.MyPickCafe.domain.MenuTag;
import com.example.MyPickCafe.domain.MoodTag;
import com.example.MyPickCafe.domain.PurposeTag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NoArgsConstructor
@Entity
@Table(name = "cafe_tag")
@Getter
@Setter
@AllArgsConstructor
public class CafeTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cafe_tag_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cafe cafe;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood_tag", length = 20)
    private MoodTag moodTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose_tag", length = 20)
    private PurposeTag purposeTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "menu_tag", length = 20)
    private MenuTag menuTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_tag", length = 20)
    private FacilityTag facilityTag;
}