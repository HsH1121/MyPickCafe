package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeCreateRequest;
import com.example.MyPickCafe.dto.CafeResponse;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CafeService")
class CafeServiceTest {

    @Mock private CafeRepository cafeRepository;
    @Mock private CafePhotoRepository cafePhotoRepository;
    @Mock private CafeInfoRepository cafeInfoRepository;
    @Mock private CafeTagRepository cafeTagRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private NotificationService notificationService;

    @InjectMocks private CafeService cafeService;

    private static final CafeCreateRequest REQUEST = new CafeCreateRequest(
            "새로운카페", "서울시 마포구 테스트로 1", 37.55, 126.92, "02-111-2222", "CAFE");

    private Member owner() {
        Member m = new Member();
        m.setId(7L);
        m.setNickname("점주");
        return m;
    }

    @Test
    @DisplayName("카페 등록 시 승인 상태는 클라이언트 입력과 무관하게 PENDING으로 시작한다")
    void createForcesPendingStatus() {
        when(memberRepository.findById(7L)).thenReturn(Optional.of(owner()));
        when(cafeRepository.existsByName(anyString())).thenReturn(false);
        when(cafeRepository.existsByNumber(anyString())).thenReturn(false);
        when(cafeRepository.save(any(Cafe.class))).thenAnswer(inv -> {
            Cafe c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CafeResponse saved = cafeService.createFromRequest(REQUEST, 7L);

        assertThat(saved.status())
                .as("등록 직후에는 관리자 승인 전이므로 PENDING이어야 한다")
                .isEqualTo(CafeStatus.PENDING.name());
    }

    @Test
    @DisplayName("카페 등록 시 소유자는 전달받은 인증 주체로 설정되고 조회수는 0으로 시작한다")
    void createAssignsOwnerAndZeroViews() {
        when(memberRepository.findById(7L)).thenReturn(Optional.of(owner()));
        when(cafeRepository.existsByName(anyString())).thenReturn(false);
        when(cafeRepository.existsByNumber(anyString())).thenReturn(false);
        when(cafeRepository.save(any(Cafe.class))).thenAnswer(inv -> {
            Cafe c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CafeResponse saved = cafeService.createFromRequest(REQUEST, 7L);

        assertThat(saved.ownerId()).isEqualTo(7L);
        assertThat(saved.ownerNickname()).isEqualTo("점주");
        assertThat(saved.views()).isZero();
    }

    @Test
    @DisplayName("카페명이 중복이면 저장하지 않고 예외를 던진다")
    void createRejectsDuplicateName() {
        when(memberRepository.findById(7L)).thenReturn(Optional.of(owner()));
        when(cafeRepository.existsByName("새로운카페")).thenReturn(true);

        assertThatThrownBy(() -> cafeService.createFromRequest(REQUEST, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("카페명");

        verify(cafeRepository, never()).save(any());
    }
}
