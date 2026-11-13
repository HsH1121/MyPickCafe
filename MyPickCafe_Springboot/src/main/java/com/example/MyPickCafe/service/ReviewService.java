// src/main/java/com/example/MyPickCafe/service/ReviewService.java
package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.FacilityTag;
import com.example.MyPickCafe.domain.MenuTag;
import com.example.MyPickCafe.domain.MoodTag;
import com.example.MyPickCafe.domain.PurposeTag;
import com.example.MyPickCafe.dto.PickBotIndexRequest;
import com.example.MyPickCafe.dto.MyReviewItem;
import com.example.MyPickCafe.dto.PythonTagRequest;
import com.example.MyPickCafe.dto.PythonTagResponse;
import com.example.MyPickCafe.dto.ReviewForm;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeTag;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.entity.ReviewTag;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.CafeTagRepository;
import com.example.MyPickCafe.repository.ReviewRepository;
import com.example.MyPickCafe.repository.ReviewTagRepository;
import com.example.MyPickCafe.support.NotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CafePhotoService cafePhotoService;
    private final NotificationService notificationService;
    private final ReviewTagRepository reviewTagRepository;
    private final CafeRepository cafeRepository;
    private final CafeTagRepository cafeTagRepository;
    private final PythonTagClient pythonTagClient;
    private final PickBotClient pickBotClient;
    /** 리뷰 작성·수정에서 AI 호출을 트랜잭션 밖에 두려고 트랜잭션 경계를 코드로 나눈다. */
    private final TransactionTemplate transactionTemplate;

    /** 메인·검색 페이지의 "최근 후기" 6건. 정렬과 개수 제한은 DB 가 맡는다. */
    @Transactional(readOnly = true)
    public List<Review> findRecent() {
        return reviewRepository.findTop6ByOrderByCreatedAtDesc();
    }

    /** 리뷰와 작성자·카페를 함께 읽는다. 리뷰 수정 화면은 OSIV 없이 트랜잭션 밖에서 둘을 확인한다. */
    @Transactional(readOnly = true)
    public Review findById(Long reviewId) {
        return reviewRepository.findWithMemberAndCafeById(reviewId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + reviewId));
    }

    // 카페 상세에서 사용할 리뷰 목록(최신순)
    @Transactional(readOnly = true)
    public List<Review> findReviewsForCafe(Long cafeId) {
        return reviewRepository.findByCafe_IdOrderByCreatedAtDesc(cafeId);
    }

    @Transactional(readOnly = true)
    public Page<MyReviewItem> findMyReviews(Long memberId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable);
        Set<Long> cafeIds = reviews.getContent().stream()
                .map(Review::getCafe)
                .filter(Objects::nonNull)
                .map(Cafe::getId)
                .collect(Collectors.toSet());
        Map<Long, String> photoUrlByCafeId = cafePhotoService.findMainPhotoUrls(cafeIds);
        return reviews.map(review -> toItem(review, photoUrlByCafeId));
    }

    private MyReviewItem toItem(Review review, Map<Long, String> photoUrlByCafeId) {
        Cafe cafe = review.getCafe();
        Long cafeId = (cafe != null ? cafe.getId() : null);
        String cafeName = (cafe != null ? cafe.getName() : "(알 수 없음)");
        String cafeMainPhotoUrl = (cafeId != null ? photoUrlByCafeId.get(cafeId) : null);

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

    /**
     * 리뷰를 저장하고 AI 태그 분석 결과를 반영한다.
     *
     * <p>트랜잭션을 둘로 나누고 AI 호출은 그 사이(트랜잭션 밖)에 둔다. 태그 분석(수 초)을 트랜잭션 안에서
     * 기다리면 그동안 DB 커넥션을 쥐고 있어, 리뷰 작성이 몰리면 커넥션 풀이 바닥나 다른 화면까지 멈춘다.
     * <ol>
     *   <li>리뷰·알림 저장 후 커밋 — 커넥션을 돌려준다</li>
     *   <li>태그 분석 — 트랜잭션 밖에서 기다린다. 실패하면 태그 없이 끝난다(예전과 같다)</li>
     *   <li>태그·감성·카페 대표 태그 반영 — 짧은 트랜잭션</li>
     * </ol>
     * 2·3 이 실패해도 리뷰는 이미 저장되어 있다. 태그 분석이 실패했을 때와 같은 상태다.
     *
     * <p>트랜잭션 안에서 부르면 안 된다. 바깥 트랜잭션(또는 OSIV)이 있으면 커넥션을 계속 쥔다
     * ({@code OpenInViewConfig} 가 이 요청 경로를 OSIV 에서 뺀다).
     */
    public Review saveWithTags(ReviewForm form, Member me, Cafe cafe) {
        Review saved = transactionTemplate.execute(status -> {
            // 컨트롤러가 다른 트랜잭션에서 읽은 카페라, 점주(지연 로딩)를 읽으려고 이 트랜잭션에서 다시 읽는다
            Cafe managedCafe = cafeRepository.findById(cafe.getId())
                    .orElseThrow(() -> new NotFoundException("카페를 찾을 수 없습니다: " + cafe.getId()));
            Review review = form.toEntity(managedCafe, me);
            if (review.getCreatedAt() == null) review.setCreatedAt(LocalDateTime.now());
            Review created = reviewRepository.save(review);

            Member owner = managedCafe.getOwner();
            if (owner != null && (me == null || !owner.getId().equals(me.getId()))) {
                try { notificationService.notifyReviewCreated(created); } catch (Exception ignore) {}
            }
            return created;
        });

        analyzeAndApplyTags(saved);
        indexForPickBot(saved);
        return saved;
    }

    /**
     * 리뷰 내용을 고치고 태그를 다시 분석한다. 트랜잭션을 나누는 이유와 순서는 {@link #saveWithTags} 와 같다.
     * 1단계에서 감성·기존 태그를 지우므로, 태그 분석이 실패하면 태그 없는 리뷰로 남는다(예전과 같다).
     */
    public Review updateWithTags(Long reviewId, ReviewForm form) {
        Review saved = transactionTemplate.execute(status -> {
            // 픽봇 인덱스에 카페 이름·주소를 보내므로 카페를 함께 읽는다
            Review review = reviewRepository.findWithMemberAndCafeById(reviewId)
                    .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + reviewId));
            review.setContent(form.getReviewContent());
            review.setSentiment(null);
            review.setGood(0);
            review.setBad(0);
            reviewTagRepository.deleteReviewTagsForReviewId(reviewId);
            return review;
        });

        analyzeAndApplyTags(saved);
        indexForPickBot(saved);
        return saved;
    }

    /** 트랜잭션 밖에서 태그 분석을 기다린 뒤, 결과가 있으면 짧은 트랜잭션으로 반영한다. */
    private void analyzeAndApplyTags(Review saved) {
        Long reviewId = saved.getId();
        Long cafeId = saved.getCafe().getId();
        pythonTagClient.analyze(new PythonTagRequest(reviewId, saved.getContent()))
                .ifPresent(res -> transactionTemplate.executeWithoutResult(status ->
                        // 분석을 기다리는 사이 리뷰가 지워졌으면 반영할 곳이 없다
                        reviewRepository.findById(reviewId).ifPresent(review -> {
                            saveEnumTags(review, res);
                            syncCafeTopTags(cafeId);
                            applySentiment(review, res.getSentiment());
                        })));
    }

    /** GOOD·BAD 만 반영한다. 리뷰는 이 트랜잭션에서 읽은 엔티티라 커밋할 때 함께 저장된다. */
    private static void applySentiment(Review review, String sentiment) {
        if (sentiment == null) return;
        String s = sentiment.trim().toUpperCase();
        if (!"GOOD".equals(s) && !"BAD".equals(s)) return;
        review.setSentiment(s);
        review.setGood("GOOD".equals(s) ? 1 : 0);
        review.setBad("BAD".equals(s) ? 1 : 0);
    }

    /** 픽봇 검색 인덱스에 리뷰를 넣는다. 응답을 기다리지 않는다(PickBotClient.indexOneAsync). */
    private void indexForPickBot(Review review) {
        pickBotClient.indexOneAsync(PickBotIndexRequest.builder()
                .reviewId(review.getId())
                .cafeId(review.getCafe().getId())
                .cafeName(review.getCafe().getName())
                .address(review.getCafe().getAddress())
                .reviewText(review.getContent())
                .build());
    }

    /** 승인된 모든 카페의 대표 태그를 다시 계산한다. SQL로 직접 넣은 리뷰처럼 리뷰 작성 흐름을 거치지 않은 데이터용. */
    @Transactional
    public int updateAllCafeTags() {
        List<Cafe> cafes = cafeRepository.findByStatus(CafeStatus.APPROVED);
        for (Cafe cafe : cafes) {
            syncCafeTopTags(cafe.getId());
        }
        return cafes.size();
    }

    private void syncCafeTopTags(Long cafeId) {
        List<Object[]> rows = reviewTagRepository.findTagCountsForCafeId(cafeId);

        // category -> [(category, code, count)] 이미 cnt 내림차순 정렬된 상태
        Map<String, List<Object[]>> byCategory = new LinkedHashMap<>();
        for (Object[] row : rows) {
            byCategory.computeIfAbsent((String) row[0], k -> new ArrayList<>()).add(row);
        }

        cafeTagRepository.deleteCafeTagsForCafeId(cafeId);

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
