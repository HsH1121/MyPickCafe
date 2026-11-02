package com.example.MyPickCafe.service;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeResponse;
import com.example.MyPickCafe.support.NotFoundException;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Favorite;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.FavoriteRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final CafeRepository cafeRepository;

    /**
     * 즐겨찾기 추가/해제. 추가는 승인된 카페만 된다(아니면 없는 카페와 같은 404).
     * 해제는 카페 상태와 관계없이 된다 — 즐겨찾기한 뒤 승인이 취소된 카페도 지울 수 있어야 한다.
     */
    @Transactional
    public boolean toggle(Long memberId, Long cafeId) {
        return favoriteRepository.findByMember_IdAndCafe_Id(memberId, cafeId)
                .map(f -> { favoriteRepository.delete(f); return false; })
                .orElseGet(() -> {
                    Cafe cafe = cafeRepository.findById(cafeId)
                            .filter(Cafe::isApproved)
                            .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));
                    Favorite f = new Favorite();
                    f.setMember(memberRepository.getReferenceById(memberId));
                    f.setCafe(cafe);
                    favoriteRepository.save(f);
                    return true;
                });
    }

    public long countByCafe(Long cafeId) { return favoriteRepository.countByCafe_Id(cafeId); }

    public boolean exists(Long memberId, Long cafeId) {
        return favoriteRepository.existsByMember_IdAndCafe_Id(memberId, cafeId);
    }

    @Transactional(readOnly = true)
    public long countFavoriteForCafe(Long cafeId) {
        Cafe c = cafeRepository.getReferenceById(cafeId);
        return favoriteRepository.countByCafe(c);
    }

    /** 내 즐겨찾기 — 승인된 카페만. */
    @Transactional(readOnly = true)
    public Page<Cafe> listMyFavorites(Long memberId, Pageable pageable) {
        return favoriteRepository.findByMember_IdAndCafe_Status(memberId, CafeStatus.APPROVED, pageable)
                .map(Favorite::getCafe);
    }

    /** REST 응답용 — 엔티티 대신 DTO 로 내려 점주 회원 정보·사업자 증빙 경로가 나가지 않게 한다. */
    @Transactional(readOnly = true)
    public Page<CafeResponse> listMyFavoritesForApi(Long memberId, Pageable pageable) {
        return listMyFavorites(memberId, pageable).map(CafeResponse::from);
    }

    @Transactional(readOnly = true)
    public boolean isFavoritedByEmail(String memberEmail, Long cafeId) {
        return memberRepository.findByEmail(memberEmail)
                .map(me -> {
                    Cafe c = cafeRepository.getReferenceById(cafeId);
                    return favoriteRepository.existsByMemberAndCafe(me, c);
                })
                .orElse(false);
    }
}
