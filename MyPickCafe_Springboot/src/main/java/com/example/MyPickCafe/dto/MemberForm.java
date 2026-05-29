package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Member;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MemberForm {

    // 업데이트 시에만 사용; 생성 시 null
    @JsonAlias({"memberId"})
    private Long id;

    @JsonAlias({"memberEmail"})
    private String email;

    @JsonAlias({"memberPassword"})
    private String password;

    @JsonAlias({"memberNickname"})
    private String nickname;

    @JsonAlias({"memberAge"})
    private Long age;

    @JsonAlias({"memberGender"})
    private String gender;

    @JsonAlias({"memberRoleKind"})
    private String roleKind;

    private LocalDateTime createdAt;

    @JsonAlias({"memberPhoto"})
    private String photo;

    @JsonAlias({"memberTokenVersion"})
    private Long tokenVersion;

    public Member toEntity() {
        Member m = new Member();
        m.setId(id); // null이면 새로 생성
        m.setEmail(email);
        m.setPassword(password);
        m.setNickname(nickname);
        m.setAge(age);
        m.setGender("M".equalsIgnoreCase(gender) ? "M" : ("F".equalsIgnoreCase(gender) ? "F" : null));
        RoleKind rk = RoleKind.MEMBER;
        if (roleKind != null && !roleKind.isBlank()) {
            try { rk = RoleKind.valueOf(roleKind.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        m.setRoleKind(rk);
        m.setPhoto(photo);
        if (tokenVersion != null) m.setTokenVersion(tokenVersion);
        return m;
    }
}
