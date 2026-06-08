// src/main/java/com/example/MyPickCafe/service/ReviewService.java
package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.FacilityTag;
import com.example.MyPickCafe.domain.MenuTag;
import com.example.MyPickCafe.domain.MoodTag;
import com.example.MyPickCafe.domain.PurposeTag;
import com.example.MyPickCafe.dto.ChatbotIndexRequest;
import com.example.MyPickCafe.dto.MyReviewItem;
import com.example.MyPickCafe.dto.PythonTagRequest;
import com.example.MyPickCafe.dto.PythonTagResponse;
import com.example.MyPickCafe.dto.ReviewCreateForm;
import com.example.MyPickCafe.dto.ReviewForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeTag;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.entity.ReviewTag;
import com.example.MyPickCafe.repository.CafePhotoRepository;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.CafeTagRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import com.example.MyPickCafe.repository.ReviewTagRepository;
import com.example.MyPickCafe.support.EntityIdUtil;
import com.example.MyPickCafe.support.NotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CafePhotoRepository cafePhotoRepository;
    private final NotificationService notificationService;
    private final ReviewTagRepository reviewTagRepository;
    private final CafeRepository cafeRepository;
    private final CafeTagRepository cafeTagRepository;
    private final MemberRepository memberRepository;
    private final PythonTagClient pythonTagClient;
    private final ChatbotClient chatbotClient;

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
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found: " + id));
        Long cafeId = review.getCafe().getId();
        chatbotClient.deleteOneAsync(id);
        reviewRepository.deleteById(id);
        syncCafeTopTags(cafeId);
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

        String cafeMainPhotoUrl = null;
        if (cafe != null) {
            var photo = cafePhotoRepository.findMainPhoto(cafe.getId());
            cafeMainPhotoUrl = (photo != null) ? photo.getUrl() : null;
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
        r.setContent(form.getReviewContent());

        return r;
    }
    @Transactional
    public Review saveWithTags(ReviewForm form, Member me, Cafe cafe) {
        // 1. 리뷰 저장
        Review review = form.toEntity(cafe, me);
        if (review.getCreatedAt() == null) review.setCreatedAt(LocalDateTime.now());
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
            syncCafeTopTags(saved.getCafe().getId());
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

        chatbotClient.indexOneAsync(ChatbotIndexRequest.builder()
                .reviewId(saved.getId())
                .cafeId(saved.getCafe().getId())
                .cafeName(saved.getCafe().getName())
                .address(saved.getCafe().getAddress())
                .reviewText(saved.getContent())
                .build());

        return saved;
    }

    @Transactional
    public Review updateWithTags(Long reviewId, ReviewForm form) {
        // 1. 기존 리뷰 조회 및 필드 업데이트
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));

        review.setContent(form.getReviewContent());
        review.setSentiment(null);
        review.setGood(0);
        review.setBad(0);
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
            syncCafeTopTags(saved.getCafe().getId());
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

        chatbotClient.indexOneAsync(ChatbotIndexRequest.builder()
                .reviewId(saved.getId())
                .cafeId(saved.getCafe().getId())
                .cafeName(saved.getCafe().getName())
                .address(saved.getCafe().getAddress())
                .reviewText(saved.getContent())
                .build());

        return saved;
    }

    private void syncCafeTopTags(Long cafeId) {
        List<Object[]> rows = reviewTagRepository.findTagCountsByCafe(cafeId);

        // category -> [(code, count)] 이미 cnt 내림차순 정렬된 상태
        Map<String, List<Object[]>> byCategory = new LinkedHashMap<>();
        for (Object[] row : rows) {
            byCategory.computeIfAbsent((String) row[0], k -> new ArrayList<>()).add(row);
        }

        cafeTagRepository.deleteAllByCafeId(cafeId);

        Cafe cafeRef = cafeRepository.getReferenceById(cafeId);

        for (Map.Entry<String, List<Object[]>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Object[]> tagCounts = entry.getValue();
            long topCount = ((Number) tagCounts.get(0)[2]).longValue();

            for (Object[] tagCount : tagCounts) {
                long cnt = ((Number) tagCount[2]).longValue();
                if (cnt < topCount * 0.85) break;

                String code = (String) tagCount[1];
                CafeTag cafeTag = new CafeTag();
                cafeTag.setCafe(cafeRef);

                switch (category) {
                    case "FACILITY" -> cafeTag.setFacilityTag(parseEnum(FacilityTag.class, code));
                    case "MENU"     -> cafeTag.setMenuTag(parseEnum(MenuTag.class, code));
                    case "PURPOSE"  -> cafeTag.setPurposeTag(parseEnum(PurposeTag.class, code));
                    case "MOOD"     -> cafeTag.setMoodTag(parseEnum(MoodTag.class, code));
                }

                if (cafeTag.getFacilityTag() != null || cafeTag.getMenuTag() != null
                        || cafeTag.getPurposeTag() != null || cafeTag.getMoodTag() != null) {
                    cafeTagRepository.save(cafeTag);
                }
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null) return null;
        try { return Enum.valueOf(clazz, value); } catch (IllegalArgumentException e) { return null; }
    }

    private void saveEnumTags(Review saved, PythonTagResponse res) {
        Map<String, List<String>> tagMap = new LinkedHashMap<>();
        tagMap.put("FACILITY", res.getFacilityTags() != null ? res.getFacilityTags() : List.of());
        tagMap.put("MENU",     res.getMenuTags()     != null ? res.getMenuTags()     : List.of());
        tagMap.put("PURPOSE",  res.getPurposeTags()  != null ? res.getPurposeTags()  : List.of());
        tagMap.put("MOOD",     res.getMoodTags()     != null ? res.getMoodTags()     : List.of());
        tagMap.forEach((category, codes) ->
            codes.forEach(val ->
                reviewTagRepository.save(new ReviewTag(null, saved, category, val.trim().toUpperCase()))
            )
        );
    }

}
