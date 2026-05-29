package com.example.MyPickCafe.domain;

public enum PurposeTag {
    STUDY("공부"),
    TALK("대화"),
    REST("휴식"),
    DATE("데이트"),
    PHOTO("사진"),
    MEETING("미팅");

    private final String label;

    PurposeTag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
