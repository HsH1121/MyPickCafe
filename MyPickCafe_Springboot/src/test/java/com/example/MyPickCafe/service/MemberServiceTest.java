package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.dto.MemberCreateRequest;
import com.example.MyPickCafe.dto.MemberUpdateRequest;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

    @Mock private MemberRepository repository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private MemberService memberService;

    private MemberCreateRequest request(String email, String nickname, String role) {
        return new MemberCreateRequest(email, "rawPassword123", nickname, 25L, "M", role, null);
    }

    @Test
    @DisplayName("회원 생성 시 비밀번호를 평문이 아닌 해시로 저장한다")
    void createMemberEncodesPassword() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("$2a$10$encoded");
        when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.createMember(request("new@example.com", "newbie", null));

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(repository).save(saved.capture());

        assertThat(saved.getValue().getPassword())
                .as("평문 비밀번호가 그대로 저장되면 안 된다")
                .isEqualTo("$2a$10$encoded")
                .isNotEqualTo("rawPassword123");
    }

    @Test
    @DisplayName("역할을 지정하지 않으면 MEMBER로 생성된다")
    void createMemberDefaultsToMemberRole() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.createMember(request("new@example.com", "newbie", null));

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRoleKind()).isEqualTo(RoleKind.MEMBER);
    }

    @Test
    @DisplayName("중복 이메일이면 저장하지 않고 예외를 던진다")
    void createMemberRejectsDuplicateEmail() {
        when(repository.findByEmail("dup@example.com")).thenReturn(Optional.of(new Member()));

        assertThatThrownBy(() -> memberService.createMember(request("dup@example.com", "someone", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("알 수 없는 역할 문자열이면 예외를 던진다")
    void createMemberRejectsUnknownRole() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.existsByNickname(anyString())).thenReturn(false);

        assertThatThrownBy(() -> memberService.createMember(request("new@example.com", "newbie", "SUPERUSER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("권한");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("수정 시 null 필드는 기존 값을 덮어쓰지 않는다")
    void updateMemberIgnoresNullFields() {
        Member existing = new Member();
        existing.setId(1L);
        existing.setNickname("original");
        existing.setAge(20L);
        existing.setGender("M");
        existing.setPassword("$2a$10$original");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        // 나이만 변경, 나머지는 null
        memberService.updateMember(1L, new MemberUpdateRequest(null, 33L, null, null, null));

        assertThat(existing.getAge()).isEqualTo(33L);
        assertThat(existing.getNickname()).isEqualTo("original");
        assertThat(existing.getGender()).isEqualTo("M");
        assertThat(existing.getPassword()).isEqualTo("$2a$10$original");
    }
}
