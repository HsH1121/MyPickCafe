package com.example.MyPickCafe.entity;

import com.example.MyPickCafe.domain.RoleKind;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false, unique = true)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * BCrypt 해시. 어떤 경우에도 JSON 응답에 실려서는 안 되므로
     * DTO 분리와 별개로 엔티티 레벨에서도 직렬화를 차단한다(다중 방어).
     * 역직렬화는 허용해야 로그인/가입 폼 바인딩이 동작한다.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    @PrePersist
    public void prePersist() {
        if (roleKind == null) roleKind = RoleKind.MEMBER; // 기본값
        if (tokenVersion == null) tokenVersion = 0L;
    }
}
