// src/main/java/com/example/MyPickCafe/service/CafeService.java
package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeCardForm;
import com.example.MyPickCafe.dto.CafeCreateRequest;
import com.example.MyPickCafe.dto.CafeForm;
import com.example.MyPickCafe.dto.CafeResponse;
import com.example.MyPickCafe.dto.CafeUpdateRequest;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafeInfoRepository;
import com.example.MyPickCafe.repository.CafePhotoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.CafeTagRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CafeService {

    private static final String PLACEHOLDER = "/images/placeholder-cafe.jpg";

    private final CafeRepository cafeRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final CafeInfoRepository cafeInfoRepository;
    private final CafeTagRepository cafeTagRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
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

    // ===== REST API용 조회 (엔티티 대신 DTO 반환) =====
    // 매핑을 트랜잭션 안에서 끝내므로 open-in-view에 의존하지 않는다.

    @Transactional(readOnly = true)
    public List<CafeResponse> findAllForApi() {
        return cafeRepository.findAll().stream().map(CafeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CafeResponse findByIdForApi(Long id) {
        return CafeResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<CafeResponse> findByStatusForApi(CafeStatus status) {
        return cafeRepository.findByStatus(status).stream().map(CafeResponse::from).toList();
    }

    // 생성/수정/삭제
    @Transactional
    public CafeResponse createFromRequest(CafeCreateRequest req, Long ownerId) {
        Member owner = memberRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Member not found: " + ownerId));

        if (cafeRepository.existsByName(req.name())) {
            throw new IllegalArgumentException("이미 존재하는 카페명입니다.");
        }
        if (cafeRepository.existsByNumber(req.phone())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }

        Cafe c = new Cafe();
        c.setOwner(owner);                  // 소유자는 인증 주체에서 가져온다
        c.setName(req.name());
        c.setAddress(req.address());
        c.setLat(req.lat());
        c.setLon(req.lon());
        c.setNumber(req.phone());
        c.setCode(req.code());
        c.setDate(LocalDateTime.now());
        c.setViews(0L);
        c.setStatus(CafeStatus.PENDING);    // 승인 상태는 항상 서버가 결정

        Cafe saved = cafeRepository.save(c);
        try { notificationService.notifyAdminCafeRegistered(saved); } catch (Exception ignore) {}
        return CafeResponse.from(saved);
    }

    @Transactional
    public CafeResponse updateFromRequest(Long id, CafeUpdateRequest req) {
        Cafe c = cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));

        if (req.name() != null && !req.name().isBlank())       c.setName(req.name());
        if (req.address() != null && !req.address().isBlank()) c.setAddress(req.address());
        if (req.lat() != null)                                 c.setLat(req.lat());
        if (req.lon() != null)                                 c.setLon(req.lon());
        if (req.phone() != null && !req.phone().isBlank())     c.setNumber(req.phone());
        if (req.code() != null && !req.code().isBlank())        c.setCode(req.code());

        return CafeResponse.from(c);        // 더티 체킹으로 반영
    }

    @Transactional
    public void delete(Long id) {
        if (!cafeRepository.existsById(id)) throw new NotFoundException("Cafe not found: " + id);
        cafeRepository.deleteById(id);
        // 파일 스토리지 정리 필요시 fileStorageService.delete(...) 추가
    }

    // 생성 (폼 + 파일 업로드)
    @Transactional
    public Long createCafe(Long cafeOwnerId,
                           CafeForm form,
                           List<MultipartFile> cafePhotoFiles,
                           MultipartFile bizDocFile) throws AccessDeniedException {

        // 1) 소유자 확인
        Member cafeOwner = memberRepository.findById(cafeOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 필수값 검증
        if (form.getName() == null || form.getName().isBlank())
            throw new IllegalArgumentException("카페명을 입력해주세요.");
        if (form.getCode() == null || form.getCode().isBlank())
            throw new IllegalArgumentException("카테고리를 선택해주세요.");

        // 3) 중복 방지
        if (cafeRepository.existsByName(form.getName()))
            throw new IllegalArgumentException("이미 존재하는 카페명입니다.");
        if (form.getNumber() != null && cafeRepository.existsByNumber(form.getNumber()))
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");

        // 4) 카페 엔티티 생성
        Cafe cafe = form.toEntity();
        cafe.setStatus(CafeStatus.PENDING);
        cafe.setOwner(cafeOwner);
        Cafe saved = cafeRepository.save(cafe);

        // 4) 카페 사진 다중 업로드
        if (cafePhotoFiles != null) {
            for (MultipartFile file : cafePhotoFiles) {
                if (file == null || file.isEmpty()) continue;
                String photoUrl = fileStorageService.save(file, "cafes/" + saved.getId());
                boolean hasMain = cafePhotoRepository.existsByCafe_IdAndMainTrue(saved.getId());
                int nextOrder  = (int) cafePhotoRepository.countByCafe_Id(saved.getId());
                CafePhoto photo = new CafePhoto();
                photo.setCafe(saved);
                photo.setUrl(photoUrl);
                photo.setSortIndex(nextOrder);
                photo.setMain(!hasMain);
                cafePhotoRepository.save(photo);
            }
        }

        // 5) 사업자 증빙 파일
        if (bizDocFile != null && !bizDocFile.isEmpty()) {
            String docUrl = fileStorageService.save(bizDocFile, "cafes/" + saved.getId() + "/docs");
            saved.setBizDoc(docUrl);
        }

        // 6) CafeInfo 저장
        CafeInfo info = new CafeInfo();
        info.setCafe(saved);
        info.setOpenTime(form.getOpenTime());
        info.setCloseTime(form.getCloseTime());
        info.setHoliday(form.getHoliday());
        info.setNotice(form.getNotice());
        info.setInfo(form.getInfo());
        cafeInfoRepository.save(info);

        // 7) 관리자 알림
        try { notificationService.notifyAdminCafeRegistered(saved); } catch (Exception ignore) {}

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
        Cafe c = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("Cafe not found: " + cafeId));
        c.setStatus(CafeStatus.APPROVED);
        try { notificationService.notifyCafeStatus(c, CafeStatus.APPROVED); } catch (Exception ignore) {}
        Member owner = c.getOwner();
        if (owner != null && owner.getRoleKind() == com.example.MyPickCafe.domain.RoleKind.MEMBER) {
            owner.setRoleKind(com.example.MyPickCafe.domain.RoleKind.CAFEOWNER);
            memberRepository.save(owner);
        }
    }

    @Transactional(readOnly = true)
    public List<Cafe> findByOwnerId(Long ownerId) {
        return cafeRepository.findByOwner_IdOrderByDateDesc(ownerId);
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

    @Transactional(readOnly = true)
    public List<CafeCardForm> findApprovedCardsSorted(String sort, int limit) {
        List<Cafe> cafes;
        if ("likes".equals(sort)) {
            List<Cafe> all = cafeRepository.findByStatus(CafeStatus.APPROVED);
            Set<Long> ids = all.stream().map(Cafe::getId).collect(Collectors.toSet());
            Map<Long, Double> ratioMap = new HashMap<>();
            for (Object[] row : reviewRepository.findSentimentCountsByCafeIds(ids)) {
                long cafeId = ((Number) row[0]).longValue();
                long good   = ((Number) row[1]).longValue();
                long total  = ((Number) row[2]).longValue();
                ratioMap.put(cafeId, total == 0 ? 0.0 : (double) good / total);
            }
            cafes = all.stream()
                    .sorted(Comparator.comparingDouble((Cafe c) ->
                            ratioMap.getOrDefault(c.getId(), 0.0)).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } else if ("newest".equals(sort)) {
            cafes = cafeRepository.findByStatusOrderByDateDesc(CafeStatus.APPROVED, PageRequest.of(0, limit));
        } else {
            cafes = cafeRepository.findByStatusOrderByViewsDesc(CafeStatus.APPROVED, PageRequest.of(0, limit));
        }
        return enrichWithPhotos(cafes);
    }

    @Transactional(readOnly = true)
    public List<CafeCardForm> findApprovedCardsByTag(String category, String code, int limit) {
        List<Long> ids = cafeTagRepository.findCafeIdsByTag(category, code);
        if (ids.isEmpty()) return List.of();
        return enrichWithPhotos(
                cafeRepository.findByStatusAndIdInOrderByViewsDesc(CafeStatus.APPROVED, ids)
                        .stream().limit(limit).collect(Collectors.toList()));
    }

    private List<CafeCardForm> enrichWithPhotos(List<Cafe> cafes) {
        if (cafes.isEmpty()) return List.of();
        Set<Long> ids = cafes.stream().map(Cafe::getId).collect(Collectors.toSet());
        Map<Long, String> photoMap = new HashMap<>();
        for (CafePhoto p : cafePhotoRepository.findForCafeIdsOrderByMainThenSort(ids)) {
            photoMap.putIfAbsent(p.getCafe().getId(), p.getUrl());
        }
        return cafes.stream()
                .map(c -> new CafeCardForm(
                        c.getId(), c.getName(), c.getAddress(),
                        c.getNumber(), c.getCode(), c.getViews(),
                        photoMap.getOrDefault(c.getId(), PLACEHOLDER)))
                .collect(Collectors.toList());
    }
}

