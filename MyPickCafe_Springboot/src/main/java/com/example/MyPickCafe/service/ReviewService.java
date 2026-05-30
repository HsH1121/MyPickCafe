// src/main/java/com/example/MyPickCafe/service/ReviewService.java
package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.MyReviewItem;
import com.example.MyPickCafe.dto.PythonTagRequest;
import com.example.MyPickCafe.dto.PythonTagResponse;
import com.example.MyPickCafe.dto.ReviewCreateForm;
import com.example.MyPickCafe.dto.ReviewForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.entity.ReviewTag;
import com.example.MyPickCafe.repository.CafePhotoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import com.example.MyPickCafe.repository.ReviewTagRepository;
import com.example.MyPickCafe.support.EntityIdUtil;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final NotificationService notificationService;
    private final ReviewTagRepository reviewTagRepository;
    private final CafeRepository cafeRepository;
    private final MemberRepository memberRepository;
    private final PythonTagClient pythonTagClient;

    @Transactional(readOnly = true)
    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found: " + id));
    }

    @Transactional
    public Review create(ReviewCreateForm form, Long memberId) {
        Review saved = reviewRepository.save(mapToEntity(form, memberId));
        try {
            notificationService.notifyReviewCreated(saved);
        } catch (Exception ignore) {
        }
        return saved;
    }


    @Transactional
    public Review update(Long id, Review entity) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return reviewRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new NotFoundException("Review not found: " + id);
        }
        reviewRepository.deleteById(id);
    }

    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    // 카페 상세에서 사용할 리뷰 목록(최신순)
    @Transactional(readOnly = true)
    public List<Review> findByCafeIdWithMember(Long cafeId) {
        return reviewRepository.findByCafe_IdOrderByCreatedAtDesc(cafeId);
    }

    // 홈의 최근 올라온 후기
    @Transactional(readOnly = true)
    public List<Review> findRecentTop10() {
        return reviewRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public Page<MyReviewItem> findMyReviews(Long memberId, Pageable pageable) {
        return reviewRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toItem);
    }

    private MyReviewItem toItem(Review review) {
        Cafe cafe = review.getCafe();
        Long cafeId = (cafe != null ? cafe.getId() : null);
        String cafeName = (cafe != null ? cafe.getName() : "(알 수 없음)");

        // 카페 사진 url
        String cafeMainPhotoUrl = cafePhotoRepository.findMainPhoto(cafe.getId()).getUrl();
        try {
            // 예시: c.getCafeThumb() 가 있으면 /images/cafe/{file} 로 만든다
            var method = Cafe.class.getMethod("getCafeThumb");
            Object v = (cafe != null ? method.invoke(cafe) : null);
            if (v != null) {
                String s = String.valueOf(v);
                if (!s.isBlank()) cafeMainPhotoUrl = "/images/cafe/" + s;
            }
        } catch (Exception ignore) {
            // 필드가 없으면 null -> 템플릿에서 NO IMG
        }

        return new MyReviewItem(
                review.getId(),
                cafeId,
                cafeName,
                cafeMainPhotoUrl,
                safe(review.getContent()),
                review.getCreatedAt(),
                review.getGood(),
                review.getBad()
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private Review mapToEntity(ReviewCreateForm form, Long memberId) {
        Review r = new Review();
        r.setCafe(cafeRepository.getReferenceById(form.getCafeId()));
        r.setMember(memberRepository.getReferenceById(memberId));

        // ⬇️ 아래 3줄은 네 엔티티/폼 필드명에 맞게 필요하면 이름만 바꿔줘!
        r.setContent(form.getReviewContent());         // 예: form.getText() / r.setText(...)
        r.setSentiment(
                form.getSentiment() != null ? form.getSentiment() : "GOOD"
        );                                       // 감성 필드명 다르면 맞게 변경

        return r;
    }
    @Transactional
    public Review saveWithTags(ReviewForm form, Member me, Cafe cafe, String normalizedSentiment) {
        // 1. 리뷰 저장
        Review review = form.toEntity(cafe, me);
        if (review.getCreatedAt() == null) review.setCreatedAt(LocalDateTime.now());
        review.setSentiment(normalizedSentiment);
        if ("GOOD".equals(normalizedSentiment)) { review.setGood(1); review.setBad(0); }
        else if ("BAD".equals(normalizedSentiment)) { review.setGood(0); review.setBad(1); }
        Review saved = reviewRepository.save(review);

        if (cafe.getOwner() != null && (me == null || !cafe.getOwner().getId().equals(me.getId()))) {
            try { notificationService.notifyReviewCreated(saved); } catch (Exception ignore) {}
        }

        // 2. FastAPI 태그 추출 (저장 후 호출, 실패 시 태그 없이 진행)
        PythonTagRequest pyReq = PythonTagRequest.builder()
                .reviewId(saved.getId())
                .reviewText(saved.getContent())
                .build();

        pythonTagClient.analyze(pyReq).ifPresent(res -> {
            saveEnumTags(saved, res);
            if (res.getSentiment() != null) {
                String s = res.getSentiment().trim().toUpperCase();
                if ("GOOD".equals(s) || "BAD".equals(s)) {
                    saved.setSentiment(s);
                    saved.setGood("GOOD".equals(s) ? 1 : 0);
                    saved.setBad("BAD".equals(s)  ? 1 : 0);
                    reviewRepository.save(saved);
                }
            }
        });

        return saved;
    }

    @Transactional
    public Review updateWithTags(Long reviewId, ReviewForm form, String normalizedSentiment) {
        // 1. 기존 리뷰 조회 및 필드 업데이트
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));

        review.setContent(form.getReviewContent());
        review.setSentiment(normalizedSentiment);
        if ("GOOD".equals(normalizedSentiment)) { review.setGood(1); review.setBad(0); }
        else if ("BAD".equals(normalizedSentiment)) { review.setGood(0); review.setBad(1); }
        Review saved = reviewRepository.save(review);

        // 2. 기존 태그 전부 삭제
        reviewTagRepository.deleteByReviewId(reviewId);

        // 3. FastAPI 태그 재추출
        PythonTagRequest pyReq = PythonTagRequest.builder()
                .reviewId(saved.getId())
                .reviewText(saved.getContent())
                .build();

        pythonTagClient.analyze(pyReq).ifPresent(res -> {
            saveEnumTags(saved, res);
            if (res.getSentiment() != null) {
                String s = res.getSentiment().trim().toUpperCase();
                if ("GOOD".equals(s) || "BAD".equals(s)) {
                    saved.setSentiment(s);
                    saved.setGood("GOOD".equals(s) ? 1 : 0);
                    saved.setBad("BAD".equals(s)  ? 1 : 0);
                    reviewRepository.save(saved);
                }
            }
        });

        return saved;
    }

    private void saveEnumTags(Review saved, PythonTagResponse res) {
        if (res.getFacilityTags() != null)
            for (String val : res.getFacilityTags())
                reviewTagRepository.save(new ReviewTag(null, saved, "FACILITY", val.trim().toUpperCase()));
        if (res.getMenuTags() != null)
            for (String val : res.getMenuTags())
                reviewTagRepository.save(new ReviewTag(null, saved, "MENU", val.trim().toUpperCase()));
        if (res.getPurposeTags() != null)
            for (String val : res.getPurposeTags())
                reviewTagRepository.save(new ReviewTag(null, saved, "PURPOSE", val.trim().toUpperCase()));
        if (res.getMoodTags() != null)
            for (String val : res.getMoodTags())
                reviewTagRepository.save(new ReviewTag(null, saved, "MOOD", val.trim().toUpperCase()));
    }

}
