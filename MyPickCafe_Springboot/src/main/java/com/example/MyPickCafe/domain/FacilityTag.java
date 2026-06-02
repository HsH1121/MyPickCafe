package com.example.MyPickCafe.domain;

public enum FacilityTag implements TagEnum {
    PLUG    ("콘센트"),
    TERRACE ("테라스"),
    PET     ("반려동물"),
    PARKING ("주차"),
    WIFI    ("와이파이");

    private final String label;

    FacilityTag(String label) {
        this.label = label;
    }

    @Override public String getLabel()         { return label; }
    @Override public String getCategory()      { return "FACILITY"; }
    @Override public String getCategoryLabel() { return "시설"; }
}
