// src/main/java/com/example/MyPickCafe/service/CafeService.java
package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafePhotoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.support.EntityIdUtil;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeService {

    private final CafeRepository cafeRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<Cafe> findAll() {
        return cafeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cafe> findTop8ByViews() {
        return cafeRepository.findTop8ByOrderByViewsDesc(); // DB에서 정렬+TOP8
    }

    @Transactional(readOnly = true)
    public Cafe findById(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Cafe> findByStatus(CafeStatus status) {
        return cafeRepository.findByStatus(status);
    }

    // 생성/수정/삭제
    @Transactional
    public Cafe create(Cafe entity) {
        EntityIdUtil.setId(entity, null);              // 신규는 id null
        if (entity.getStatus() == null) entity.setStatus(CafeStatus.PENDING);
        return cafeRepository.save(entity);
    }

    @Transactional
    public Cafe update(Long id, Cafe patch) {
        Cafe c = cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));

        if (patch.getName() != null) c.setName(patch.getName());
        if (patch.getAddress() != null) c.setAddress(patch.getAddress());
        if (patch.getLat() != null) c.setLat(patch.getLat());
        if (patch.getLon() != null) c.setLon(patch.getLon());
        if (patch.getNumber() != null) c.setNumber(patch.getNumber());
        if (patch.getCode() != null) c.setCode(patch.getCode());
        try {
            if (patch.getBizDoc() != null) c.setBizDoc(patch.getBizDoc());
        } catch (Exception ignored) {
        }
        return c;
    }

    @Transactional
    public void delete(Long id) {
        if (!cafeRepository.existsById(id)) throw new NotFoundException("Cafe not found: " + id);
        cafeRepository.deleteById(id);
        // 파일 스토리지 정리 필요시 fileStorageService.delete(...) 추가
    }

    // 생성 (폼 + 파일 업로드)
    @Transactional
    public Long createCafe(Long cafeOwnerId, CafeForm form) throws AccessDeniedException {
        return createCafe(cafeOwnerId, form, null, null);
    }

    @Transactional
    public Long createCafe(Long cafeOwnerId,
                           CafeForm form,
                           MultipartFile cafePhotoFile,
                           MultipartFile bizDocFile) throws AccessDeniedException {

        // 1) 소유자 확인
        Member cafeOwner = memberRepository.findById(cafeOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 중복 방지(선택)
        if (form.getName() != null && cafeRepository.existsByName(form.getName()))
            throw new IllegalArgumentException("이미 존재하는 카페명입니다.");
        if (form.getNumber() != null && cafeRepository.existsByNumber(form.getNumber()))
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");

        // 3) 엔티티 생성 + 소유자 연관관계 지정
        Cafe cafe = form.toEntity();
        cafe.setStatus(CafeStatus.PENDING);
        cafe.setOwner(cafeOwner);                 // // Member 연관관계로 설정

        Cafe saved = cafeRepository.save(cafe);

        // 4) 카페 사진 업로드 → CafePhoto INSERT
        if (cafePhotoFile != null && !cafePhotoFile.isEmpty()) {
            String photoUrl = fileStorageService.save(cafePhotoFile, "cafes/" + saved.getId());

            boolean hasMain = cafePhotoRepository.existsByCafe_IdAndMainTrue(saved.getId()); // 첫 업로드면 자동 메인
            int nextOrder = (int) cafePhotoRepository.countByCafe_Id(saved.getId());                // 정렬값

            CafePhoto photo = new CafePhoto();
            photo.setCafe(saved);                     // // FK 연결
            photo.setUrl(photoUrl);                   // // 저장 경로/URL
            photo.setSortIndex(nextOrder);         // // 정렬용 숫자(필드명이 int라 여기에 매핑)
            photo.setMain(!hasMain);             // // 기존 메인 없으면 이번 것을 메인으로

            cafePhotoRepository.save(photo);
        }

        // 5) 사업자 증빙 파일 업로드(선택)
        if (bizDocFile != null && !bizDocFile.isEmpty()) {
            String docUrl = fileStorageService.save(bizDocFile, "cafes/" + saved.getId() + "/docs");
            try {
                saved.setBizDoc(docUrl);
            } catch (Exception ignored) {
            }
        }

        return saved.getId();
    }

    // 대표 사진 교체(새 URL을 대표로 추가)
    @Transactional
    public void updateCafePhoto(Long cafeId, String photoUrl) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));

        // 기존 메인 해제
        List<CafePhoto> all = cafePhotoRepository.findByCafe_Id(cafeId);
        for (CafePhoto p : all) p.setMain(false);

        // 새 엔티티 생성 → 메인 지정
        CafePhoto photo = new CafePhoto();
        photo.setCafe(c);
        photo.setUrl(photoUrl);
        photo.setMain(true);
        photo.setSortIndex(all.size());           // // 마지막 뒤에 정렬

        cafePhotoRepository.save(photo);             // // 컬렉션 카스케이드에 의존하지 않고 명시 저장
    }

    @Transactional
    public void updateCafeBizDoc(Long cafeId, String docUrl) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));
        try {
            c.setBizDoc(docUrl);
        } catch (Throwable e) {
            throw new IllegalStateException("Cafe 엔티티에 setBizDoc(String)이 없습니다.", e);
        }
    }

    // 상태 변경
    @Transactional
    public void changeStatus(Long cafeId, CafeStatus status) {
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("Cafe not found: " + cafeId));
        c.setStatus(status);
        // ✅ 승인/거절 알림
        try { notificationService.notifyCafeStatus(c, status); } catch (Exception ignore) {}
    }

    @Transactional
    public void approve(Long cafeId) {
        changeStatus(cafeId, CafeStatus.APPROVED);
    }

    @Transactional
    public void reject(Long cafeId) {
        changeStatus(cafeId, CafeStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public List<Cafe> findApprovedTopByViews(int limit) {
        return cafeRepository.findByStatusOrderByViewsDesc(
                CafeStatus.APPROVED, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Cafe getOrThrow(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카페가 없습니다. id=" + id));
    }
    @Transactional(readOnly = true)
    public List<Cafe> searchApproved(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // 검색어가 없으면 승인된 카페 40개를 조회수 순으로 반환
            return cafeRepository.findByStatusOrderByViewsDesc(
                    CafeStatus.APPROVED, PageRequest.of(0, 40));
        }
        // 검색어가 있으면 이름/주소에서 검색
        return cafeRepository.findByStatusAndNameContainingOrStatusAndAddressContaining(
                CafeStatus.APPROVED, keyword, CafeStatus.APPROVED, keyword);
    }
    @Transactional(readOnly = true)
    public long countByStatus(CafeStatus status) { return cafeRepository.countByStatus(status); }

    @Transactional(readOnly = true)
    public long countAll() { return cafeRepository.count(); }
}

