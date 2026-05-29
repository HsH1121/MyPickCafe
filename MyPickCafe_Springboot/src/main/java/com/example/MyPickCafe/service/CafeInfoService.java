package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.repository.CafeInfoRepository;
import com.example.MyPickCafe.support.EntityIdUtil;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CafeInfoService {

    private final CafeInfoRepository cafeInfoRepository;

    @Transactional(readOnly = true)
    public List<CafeInfo> findAll() {
        return cafeInfoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CafeInfo findById(Long id) {
        return cafeInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CafeInfo not found: " + id));
    }

    @Transactional
    public CafeInfo create(CafeInfo entity) {
        EntityIdUtil.setId(entity, null);
        return cafeInfoRepository.save(entity);
    }

    @Transactional
    public CafeInfo update(Long id, CafeInfo entity) {
        if (!cafeInfoRepository.existsById(id)) {
            throw new NotFoundException("CafeInfo not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return cafeInfoRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!cafeInfoRepository.existsById(id)) {
            throw new NotFoundException("CafeInfo not found: " + id);
        }
        cafeInfoRepository.deleteById(id);
    }
    @Transactional(readOnly = true)
    public Optional<CafeInfo> findByCafeId(Long cafeId) {
        return cafeInfoRepository.findByCafe_Id(cafeId);
    }

    @Transactional
    public CafeInfo upsertByCafeId(Long cafeId, CafeInfo entity) {
        Optional<CafeInfo> cur = cafeInfoRepository.findByCafe_Id(cafeId);
        if (cur.isPresent()) {
            // 기존 행이 있으면 id를 복사해 update로 전환
            com.example.MyPickCafe.support.EntityIdUtil.setId(entity,
                    com.example.MyPickCafe.support.EntityIdUtil.getId(cur.get()));
        }
        return cafeInfoRepository.save(entity);
    }


}
