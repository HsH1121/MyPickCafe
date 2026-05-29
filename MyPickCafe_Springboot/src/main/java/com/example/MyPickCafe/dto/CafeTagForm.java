package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.domain.FacilityTag;
import com.example.MyPickCafe.domain.MenuTag;
import com.example.MyPickCafe.domain.MoodTag;
import com.example.MyPickCafe.domain.PurposeTag;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CafeTagForm {

    private Long id;
    private Long cafeId;
    private MoodTag moodTag;
    private PurposeTag purposeTag;
    private MenuTag menuTag;
    private FacilityTag facilityTag;

    public CafeTag toNewEntity(Cafe cafe) {
        CafeTag entity = new CafeTag();
        entity.setCafe(cafe);
        entity.setMoodTag(moodTag);
        entity.setPurposeTag(purposeTag);
        entity.setMenuTag(menuTag);
        entity.setFacilityTag(facilityTag);
        return entity;
    }
}