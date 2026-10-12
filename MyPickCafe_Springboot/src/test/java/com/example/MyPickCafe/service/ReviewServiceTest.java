package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.MyReviewItem;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReviewService")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private CafePhotoRepository cafePhotoRepository;
    @Mock private CafePhotoService cafePhotoService;
    @Mock private NotificationService notificationService;
    @Mock private ReviewTagRepository reviewTagRepository;
    @Mock private CafeRepository cafeRepository;
    @Mock private CafeTagRepository cafeTagRepository;
    @Mock private PythonTagClient pythonTagClient;
    @Mock private PickBotClient pickBotClient;

    @InjectMocks private ReviewService reviewService;

    private static Cafe cafe(long id, String name) {
        Cafe c = new Cafe();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private static Review review(long id, Cafe cafe) {
        Review r = new Review();
        r.setId(id);
        r.setCafe(cafe);
        r.setContent("리뷰 " + id);
        return r;
    }

    @Test
    @DisplayName("내 리뷰 목록의 대표 사진은 리뷰마다 조회하지 않고 페이지의 카페들을 한 번에 조회한다")
    void findMyReviewsLoadsMainPhotosInOneQuery() {
        Cafe a = cafe(1L, "카페A");
        Cafe b = cafe(2L, "카페B");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(
                List.of(review(10L, a), review(11L, b), review(12L, a)), pageable, 3);

        when(reviewRepository.findByMember_IdOrderByCreatedAtDesc(7L, pageable)).thenReturn(page);
        when(cafePhotoService.findMainPhotoUrls(any())).thenReturn(Map.of(1L, "/a.jpg"));

        Page<MyReviewItem> result = reviewService.findMyReviews(7L, pageable);

        assertThat(result.getContent())
                .extracting(MyReviewItem::getCafePhotoUrl)
                .containsExactly("/a.jpg", null, "/a.jpg");
        verify(cafePhotoService, times(1)).findMainPhotoUrls(Set.of(1L, 2L));
        verify(cafePhotoRepository, never()).findMainPhoto(anyLong());
    }
}
