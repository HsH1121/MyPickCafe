package com.example.MyPickCafe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NoArgsConstructor
@Entity
@Getter
@Setter
@AllArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id", nullable = false, unique = true)
    private Long id;

    // 부모 Cafe 삭제 시 메뉴 삭제
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 연관관계
    @JoinColumn(name = "cafe_id", nullable = false)      // FK
    @OnDelete(action = OnDeleteAction.CASCADE)           // DB cascade
    private Cafe cafe;

    // MenuCategory는 OnDelete 제거 + nullable 허용
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    private MenuCategory menuCategory;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "is_new", nullable = false)
    private boolean isNew;

    @Column(name = "is_recommended", nullable = false)
    private boolean isRecommended;

    @Column(name = "photo", length = 255)
    private String photo;
}