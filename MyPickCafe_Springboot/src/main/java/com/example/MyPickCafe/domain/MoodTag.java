package com.example.MyPickCafe.domain;

public enum MoodTag implements TagEnum {
    MODERN    ("모던"),
    RETRO     ("레트로"),
    NATURE    ("자연"),
    INDUSTRIAL("인더스트리얼"),
    CLASSIC   ("클래식");

    private final String label;

    MoodTag(String label) {
        this.label = label;
    }

    @Override public String getLabel()         { return label; }
    @Override public String getCategory()      { return "MOOD"; }
    @Override public String getCategoryLabel() { return "분위기"; }
}
