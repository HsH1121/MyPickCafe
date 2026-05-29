package com.example.MyPickCafe.service;

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

    @Transactional
    public boolean toggle(Long memberId, Long cafeId) {
        return favoriteRepository.findByMember_IdAndCafe_Id(memberId, cafeId)
                .map(f -> { favoriteRepository.delete(f); return false; })
                .orElseGet(() -> {
                    Favorite f = new Favorite();
                    f.setMember(memberRepository.getReferenceById(memberId));
                    f.setCafe(cafeRepository.getReferenceById(cafeId));
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

    @Transactional(readOnly = true)
    public Page<Cafe> listMyFavorites(Long memberId, Pageable pageable) {
        Member m = memberRepository.getReferenceById(memberId);
        return favoriteRepository.findByMember(m, pageable).map(Favorite::getCafe);
    }

    @Transactional(readOnly = true)
    public boolean isFavoritedByMemberId(Long memberId, Long cafeId) {
        Member m = memberRepository.getReferenceById(memberId);
        Cafe c = cafeRepository.getReferenceById(cafeId);
        return favoriteRepository.existsByMemberAndCafe(m, c);
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
