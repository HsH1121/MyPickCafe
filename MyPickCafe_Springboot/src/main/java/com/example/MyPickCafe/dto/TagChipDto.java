package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.CafeTag;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class TagChipDto {

    private final String categoryCode;
    private final String code;

    public static List<TagChipDto> fromCafeTag(CafeTag t) {
        List<TagChipDto> chips = new ArrayList<>();
        if (t.getMoodTag()     != null) chips.add(new TagChipDto("MOOD",     t.getMoodTag().name()));
        if (t.getPurposeTag()  != null) chips.add(new TagChipDto("PURPOSE",  t.getPurposeTag().name()));
        if (t.getMenuTag()     != null) chips.add(new TagChipDto("MENU",     t.getMenuTag().name()));
        if (t.getFacilityTag() != null) chips.add(new TagChipDto("FACILITY", t.getFacilityTag().name()));
        return chips;
    }
}
