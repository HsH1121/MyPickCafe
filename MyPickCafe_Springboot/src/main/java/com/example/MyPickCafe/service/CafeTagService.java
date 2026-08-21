package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.TagChipDto;
import com.example.MyPickCafe.repository.CafeTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeTagService {

    private final CafeTagRepository repository;

    /** 화면 상단 태그 칩 목록 — 실제로 카페에 붙어 있는 태그 종류만 최대 {@code limit}개. */
    @Transactional(readOnly = true)
    public List<TagChipDto> findDistinctChips(int limit) {
        List<TagChipDto> chips = new ArrayList<>();
        for (Object[] row : repository.findDistinctTagChips()) {
            if (chips.size() >= limit) break;
            chips.add(new TagChipDto((String) row[0], (String) row[1]));
        }
        return chips;
    }
}
