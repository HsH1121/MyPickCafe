package com.example.MyPickCafe.entity;

import com.example.MyPickCafe.domain.RoleKind;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "MEMBER")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Oracle 12c+ IDENTITY 가정
    @Column(name = "member_id", nullable = false, unique = true)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(name = "age")
    private Long age;

    // 'M' 또는 'F' 등 1글자 사용 가정
    @Column(name = "gender", length = 1)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_kind", nullable = false, length = 20)
    private RoleKind roleKind;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "photo", length = 30)
    private String photo;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

    @JsonIgnore
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cafe> cafes = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<UserNeeds> userNeeds = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (roleKind == null) roleKind = RoleKind.MEMBER; // 기본값
        if (tokenVersion == null) tokenVersion = 0L;
    }
}
