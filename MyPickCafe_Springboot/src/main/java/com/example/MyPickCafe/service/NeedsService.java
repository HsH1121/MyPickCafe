package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.UserNeeds;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.repository.NeedsRepository;
import com.example.MyPickCafe.support.EntityIdUtil;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NeedsService {

    private final NeedsRepository needsRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<UserNeeds> findAll() {
        return needsRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserNeeds findById(Long userNeedsId) {
        return needsRepository.findById(userNeedsId)
                .orElseThrow(() -> new NotFoundException("사용자 니즈를 찾을 수 없습니다: " + userNeedsId));
    }

    @Transactional
    public UserNeeds create(UserNeeds userNeeds) {
        EntityIdUtil.setId(userNeeds, null);
        return needsRepository.save(userNeeds);
    }

    @Transactional
    public UserNeeds update(Long userNeedsId, UserNeeds userNeeds) {
        if (!needsRepository.existsById(userNeedsId)) {
            throw new NotFoundException("사용자 니즈를 찾을 수 없습니다: " + userNeedsId);
        }
        EntityIdUtil.setId(userNeeds, userNeedsId);
        return needsRepository.save(userNeeds);
    }

    @Transactional
    public void delete(Long userNeedsId) {
        if (!needsRepository.existsById(userNeedsId)) {
            throw new NotFoundException("사용자 니즈를 찾을 수 없습니다: " + userNeedsId);
        }
        needsRepository.deleteById(userNeedsId);
    }

    @Transactional(readOnly = true)
    public List<UserNeeds> findByMemberId(Long memberId) {
        return needsRepository.findByMember_Id(memberId);
    }

    @Transactional
    public void replaceAll(Long memberId, List<String> selectedCodes) {
        needsRepository.deleteByMember_Id(memberId);
        if (selectedCodes == null) return;
        Member memberRef = memberRepository.getReferenceById(memberId);
        for (String code : selectedCodes) {
            String[] parts = code.split(":");
            if (parts.length != 2) continue;
            UserNeeds needs = new UserNeeds();
            needs.setMember(memberRef);
            needs.setCategoryCode(parts[0]);
            needs.setCode(parts[1]);
            needs.setNecessary(false);
            needsRepository.save(needs);
        }
    }
}
