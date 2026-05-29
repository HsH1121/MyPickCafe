package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.repository.CafePhotoRepository;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CafePhotoService {

    private final CafePhotoRepository cafePhotoRepository;
    private final FileStorageService fileStorageService;

    public List<CafePhoto> findMainPhotosForAllCafes() {
        return cafePhotoRepository.findMainPhotosForAllCafes();
    }

    public List<CafePhoto> findForCafeIdsOrderByMainThenSort(Collection<Long> cafeIds) {
        return cafePhotoRepository.findForCafeIdsOrderByMainThenSort(cafeIds);
    }

    public CafePhoto findById(Long photoId) {
        return cafePhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진 없음"));
    }

    @Transactional
    public List<CafePhoto> upload(Cafe cafe, List<MultipartFile> files, boolean setMain) throws Exception {
        Long cafeId = cafe.getId();
        List<CafePhoto> existing = cafePhotoRepository.findByCafe_Id(cafeId);
        boolean hasMain = existing.stream().anyMatch(p -> Boolean.TRUE.equals(p.getMain()));
        int nextOrder = existing.stream()
                .map(CafePhoto::getSortIndex)
                .max(Comparator.nullsFirst(Integer::compareTo))
                .orElse(-1) + 1;
        boolean mainAssignedThisRound = false;
        for (MultipartFile f : files) {
            var sf = fileStorageService.store(f, "cafes/" + cafeId);
            CafePhoto img = new CafePhoto();
            img.setCafe(cafe);
            img.setUrl(sf.url());
            img.setOriginalName(sf.originalName());
            img.setContentType(sf.contentType());
            img.setSizeBytes(sf.sizeBytes());
            img.setSortIndex(nextOrder++);
            if ((!hasMain && !mainAssignedThisRound) || (setMain && !mainAssignedThisRound)) {
                cafePhotoRepository.findByCafe_IdAndMainTrue(cafeId).ifPresent(prev -> {
                    prev.setMain(false);
                    cafePhotoRepository.save(prev);
                });
                img.setMain(true);
                mainAssignedThisRound = true;
                hasMain = true;
            } else {
                img.setMain(false);
            }
            cafePhotoRepository.save(img);
        }
        return cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafeId);
    }

    @Transactional
    public List<CafePhoto> setMainPhoto(Long photoId) {
        CafePhoto target = cafePhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진 없음"));
        Cafe cafe = target.getCafe();
        cafePhotoRepository.findByCafe_IdAndMainTrue(cafe.getId()).ifPresent(prev -> {
            if (!prev.getId().equals(photoId)) {
                prev.setMain(false);
                cafePhotoRepository.save(prev);
            }
        });
        target.setMain(true);
        cafePhotoRepository.save(target);
        return cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafe.getId());
    }

    @Transactional
    public List<CafePhoto> deletePhoto(Long photoId) {
        CafePhoto target = cafePhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("사진 없음"));
        Cafe cafe = target.getCafe();
        try { fileStorageService.delete(target.getUrl()); } catch (Throwable ignored) {}
        boolean wasMain = Boolean.TRUE.equals(target.getMain());
        cafePhotoRepository.delete(target);
        if (wasMain) {
            cafePhotoRepository.findByCafe_IdAndMainTrue(cafe.getId()).orElseGet(() -> {
                var list = cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafe.getId());
                if (!list.isEmpty()) {
                    CafePhoto first = list.get(0);
                    first.setMain(true);
                    cafePhotoRepository.save(first);
                }
                return null;
            });
        }
        return cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafe.getId());
    }

    // 메인 사진 URL만 반환
    public CafePhoto getMainPhoto(Long cafeId) {
        return cafePhotoRepository.findMainPhoto(cafeId);
    }


    @Transactional(readOnly = true)
    public List<CafePhoto> list(Long cafeId) {
        return cafePhotoRepository.findByCafe_IdOrderBySortIndexAsc(cafeId);
    }

    @Transactional
    public CafePhoto add(Cafe cafe, FileStorageService.StoredFile stored) {
        CafePhoto photo = new CafePhoto();
        photo.setCafe(cafe);
        photo.setUrl(stored.url());
        photo.setOriginalName(stored.originalName());
        photo.setContentType(stored.contentType());
        photo.setSizeBytes(stored.sizeBytes());
        int next = cafePhotoRepository.findFirstByCafe_IdOrderBySortIndexDesc(cafe.getId())
                .map(p -> (p.getSortIndex() == null ? 0 : p.getSortIndex()) + 1)
                .orElse(0);
        photo.setSortIndex(next);
        return cafePhotoRepository.save(photo);
    }

    @Transactional
    public void delete(Long photoId) {
        if (!cafePhotoRepository.existsById(photoId)) throw new NotFoundException("사진 없음");
        cafePhotoRepository.deleteById(photoId);
    }

    @Transactional
    public void setMain(Long cafeId, Long photoId) {
        // 기존 대표 해제
        cafePhotoRepository.findByCafe_IdAndMainTrue(cafeId).ifPresent(p -> {
            p.setMain(false);
            cafePhotoRepository.save(p);
        });
        // 새 대표 설정
        CafePhoto p = cafePhotoRepository.findById(photoId).orElseThrow(() -> new NotFoundException("사진 없음"));
        p.setMain(true);
        cafePhotoRepository.save(p);
    }


}
