package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.support.EntityIdUtil;
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

    private final MemberRepository repository;
    private final PasswordEncoder passwordEncoder; // BCrypt 빈 등록 가정

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional
    public Member create(Member entity) {
        EntityIdUtil.setId(entity, null);
        return repository.save(entity);
    }

    @Transactional
    public Member update(Long id, Member entity) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("User not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found by email: " + email));
    }

    // ===== 여기에 추가: 프로필 수정(+선택적 비번변경), 탈퇴, 토큰버전 =====

    /**
     * 프로필 수정 + (옵션) 비밀번호 변경을 한 번에 처리.
     * @return changedPassword: 비밀번호가 실제로 변경되었으면 true
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
                && repository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 일반 프로필 변경
        if (nickname != null && !nickname.isBlank()) m.setNickname(nickname);
        if (age != null) m.setAge(age);
        if (gender != null && !gender.isBlank()) m.setGender(gender);
        if (photo != null) m.setPhoto(photo);

        // 비번 변경 (둘 다 들어온 경우에만 시도)
        boolean changePw = (currentPasswordNullable != null && !currentPasswordNullable.isBlank()
                && newPasswordNullable != null && !newPasswordNullable.isBlank());

        if (changePw) {
            if (!passwordEncoder.matches(currentPasswordNullable, m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            m.setPassword(passwordEncoder.encode(newPasswordNullable));
        }

        return false;
    }

    /** 자기 자신 탈퇴 */
    @Transactional
    public void withdrawSelf(Long memberId) {
        delete(memberId);
    }

    /** 토큰버전 증가 (내부용) */
    private void bumpTokenVersionInternal(Member m) {
        Long v = (m.getTokenVersion() == null ? 0L : m.getTokenVersion());
        m.setTokenVersion(v + 1);
    }

    public Optional<Member> findByEmailOptional(String email) {
        return repository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return repository.findByEmail(email).isPresent();
    }

    public boolean existsByNickname(String nickname) {
        return repository.existsByNickname(nickname);
    }

    public Member save(Member m) {
        return repository.save(m);
    }

    @Transactional
    public boolean bumpTokenVersion(String email) {
        return repository.findByEmail(email).map(m -> {
            long nv = (m.getTokenVersion() == null ? 0L : m.getTokenVersion()) + 1L;
            m.setTokenVersion(nv);
            repository.save(m);
            return true;
        }).orElse(false);
    }
}
