package com.example.MyPickCafe.domain;

public enum FacilityTag {
    PLUG("콘센트"),
    CHAIR("좌석"),
    TERRACE("테라스"),
    PET("반려동물"),
    PARKING("주차"),
    WIFI("와이파이");

    private final String label;

    FacilityTag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
