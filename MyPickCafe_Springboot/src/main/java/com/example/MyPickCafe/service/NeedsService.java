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

    private final NeedsRepository repository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<UserNeeds> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public UserNeeds findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Needs not found: " + id));
    }

    @Transactional
    public UserNeeds create(UserNeeds entity) {
        EntityIdUtil.setId(entity, null);
        return repository.save(entity);
    }

    @Transactional
    public UserNeeds update(Long id, UserNeeds entity) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Needs not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Needs not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UserNeeds> findByMemberId(Long memberId) {
        return repository.findByMember_Id(memberId);
    }

    @Transactional
    public void replaceAll(Long memberId, List<String> selectedCodes) {
        repository.deleteByMember_Id(memberId);
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
            repository.save(needs);
        }
    }
}
