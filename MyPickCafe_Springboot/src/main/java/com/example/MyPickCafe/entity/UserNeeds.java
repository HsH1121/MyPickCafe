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
@Table(name = "user_needs")
public class UserNeeds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_needs_id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(name = "category_code", length = 20)
    private String categoryCode;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "is_necessary", nullable = false)
    private boolean necessary;
}