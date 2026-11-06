package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.dto.MemberCreateRequest;
import com.example.MyPickCafe.dto.MemberUpdateRequest;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.security.PasswordPolicy;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt 빈 등록 가정
    private final PasswordPolicy passwordPolicy;

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다: " + id));
    }

    /**
     * 관리자용 회원 생성.
     * 비밀번호는 반드시 이 계층에서 인코딩한다 — 평문이 저장되면
     * 로그인 검증을 우회할 수 있는 경로가 생긴다.
     */
    @Transactional
    public Member createMember(MemberCreateRequest req) {
        if (memberRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByNickname(req.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        passwordPolicy.validate(req.password(), req.email(), req.nickname());

        Member m = new Member();
        m.setEmail(req.email());
        m.setPassword(passwordEncoder.encode(req.password()));
        m.setNickname(req.nickname());
        m.setAge(req.age());
        m.setGender(req.gender());
        m.setRoleKind(parseRole(req.roleKind()));
        m.setPhoto(req.photo());
        m.setTokenVersion(0L);
        return memberRepository.save(m);
    }

    /**
     * 관리자용 회원 정보 수정.
     * 더티 체킹으로 변경분만 반영하므로, 요청에 없는 필드(비밀번호 등)가
     * null로 덮어써질 위험이 없다.
     */
    @Transactional
    public Member updateMember(Long id, MemberUpdateRequest req) {
        Member m = findById(id);

        if (req.nickname() != null && !req.nickname().isBlank()
                && !req.nickname().equals(m.getNickname())) {
            if (memberRepository.existsByNickname(req.nickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            m.setNickname(req.nickname());
        }
        if (req.age() != null) m.setAge(req.age());
        if (req.gender() != null && !req.gender().isBlank()) m.setGender(req.gender());
        if (req.photo() != null) m.setPhoto(req.photo());
        if (req.roleKind() != null && !req.roleKind().isBlank()) m.setRoleKind(parseRole(req.roleKind()));

        return m;
    }

    private RoleKind parseRole(String raw) {
        if (raw == null || raw.isBlank()) return RoleKind.MEMBER;
        try {
            return RoleKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 권한입니다: " + raw);
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new NotFoundException("회원을 찾을 수 없습니다: " + id);
        }
        memberRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("해당 이메일의 회원을 찾을 수 없습니다: " + email));
    }

    // ===== 여기에 추가: 프로필 수정(+선택적 비번변경), 탈퇴, 토큰버전 =====

    /**
     * 프로필 수정 + (옵션) 비밀번호 변경을 한 번에 처리.
     *
     * <p>비밀번호 변경은 현재 비밀번호 확인이 필수다(세션·토큰을 탈취당해도 비밀번호까지 바꾸지 못하게).
     * 변경되면 tokenVersion 을 올려 이미 발급된 모든 JWT(다른 기기·브라우저 포함)를 무효화한다.
     *
     * @return 비밀번호가 실제로 변경되었으면 true — 호출부는 재로그인을 안내한다
     */
    @Transactional
    public boolean updateSelf(Long memberId,
                              String nickname,
                              Long age,
                              String gender,
                              String photo,
                              String currentPasswordNullable,
                              String newPasswordNullable) {

        Member m = findById(memberId);

        // 닉네임 변경 중복 체크
        if (nickname != null && !nickname.isBlank()
                && !nickname.equals(m.getNickname())
                && memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 비밀번호 변경 요청 검사를 프로필 반영보다 먼저 한다(실패하면 아무것도 바꾸지 않음).
        boolean wantsNewPw = newPasswordNullable != null && !newPasswordNullable.isEmpty();
        boolean hasCurrentPw = currentPasswordNullable != null && !currentPasswordNullable.isEmpty();
        if (wantsNewPw) {
            if (!hasCurrentPw) {
                throw new IllegalArgumentException("비밀번호를 바꾸려면 현재 비밀번호를 입력해주세요.");
            }
            if (!passwordEncoder.matches(currentPasswordNullable, m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            if (newPasswordNullable.equals(currentPasswordNullable)) {
                throw new IllegalArgumentException("현재 비밀번호와 다른 비밀번호를 입력해주세요.");
            }
            String nicknameAfter = (nickname != null && !nickname.isBlank()) ? nickname : m.getNickname();
            passwordPolicy.validate(newPasswordNullable, m.getEmail(), nicknameAfter);
        }

        // 일반 프로필 변경
        if (nickname != null && !nickname.isBlank()) m.setNickname(nickname);
        if (age != null) m.setAge(age);
        if (gender != null && !gender.isBlank()) m.setGender(gender);
        if (photo != null) m.setPhoto(photo);

        if (wantsNewPw) {
            m.setPassword(passwordEncoder.encode(newPasswordNullable));
            increaseTokenVersion(m);
            return true;
        }
        return false;
    }

    // 탈퇴
    @Transactional
    public void withdrawSelf(Long memberId) {
        delete(memberId);
    }

    private void increaseTokenVersion(Member m) {
        long v = (m.getTokenVersion() == null ? 0L : m.getTokenVersion());
        m.setTokenVersion(v + 1);
    }

    @Transactional
    public boolean increaseTokenVersion(String email) {
        return memberRepository.findByEmail(email).map(m -> {
            increaseTokenVersion(m);
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<Member> findByEmailOptional(String email) {
        return memberRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }

    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    public Member save(Member m) {
        return memberRepository.save(m);
    }


}
