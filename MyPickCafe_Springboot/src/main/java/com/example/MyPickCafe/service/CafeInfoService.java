package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.CafeInfoRequest;
import com.example.MyPickCafe.entity.Cafe;
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
    public CafeInfo findById(Long cafeInfoId) {
        return cafeInfoRepository.findById(cafeInfoId)
                .orElseThrow(() -> new NotFoundException("카페 영업정보를 찾을 수 없습니다: " + cafeInfoId));
    }

    @Transactional
    public CafeInfo create(CafeInfo cafeInfo) {
        EntityIdUtil.setId(cafeInfo, null);
        return cafeInfoRepository.save(cafeInfo);
    }

    @Transactional
    public CafeInfo update(Long cafeInfoId, CafeInfoRequest req) {
        CafeInfo cur = findById(cafeInfoId);
        apply(cur, req);
        return cur;
    }

    @Transactional
    public void delete(Long cafeInfoId) {
        if (!cafeInfoRepository.existsById(cafeInfoId)) {
            throw new NotFoundException("카페 영업정보를 찾을 수 없습니다: " + cafeInfoId);
        }
        cafeInfoRepository.deleteById(cafeInfoId);
    }
    @Transactional(readOnly = true)
    public Optional<CafeInfo> findByCafeId(Long cafeId) {
        return cafeInfoRepository.findByCafe_Id(cafeId);
    }

    /**
     * 카페의 영업정보를 만들거나 수정한다. 어느 행을 쓸지는 cafe 로만 정한다
     * (요청에서 id 를 받으면 다른 카페의 행을 덮어쓸 수 있다).
     */
    @Transactional
    public CafeInfo upsertByCafe(Cafe cafe, CafeInfoRequest req) {
        CafeInfo info = cafeInfoRepository.findByCafe_Id(cafe.getId()).orElseGet(() -> {
            CafeInfo created = new CafeInfo();
            created.setCafe(cafe);
            return created;
        });
        apply(info, req);
        return cafeInfoRepository.save(info);
    }

    private static void apply(CafeInfo info, CafeInfoRequest req) {
        info.setNotice(req.notice());
        info.setInfo(req.info());
        info.setOpenTime(req.openTime());
        info.setCloseTime(req.closeTime());
        info.setHoliday(req.holiday());
    }

}
