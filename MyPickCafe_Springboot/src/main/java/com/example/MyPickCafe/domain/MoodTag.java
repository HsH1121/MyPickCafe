package com.example.MyPickCafe.domain;

public enum MoodTag implements TagEnum {
    MODERN    ("모던",         "깔끔하고 세련된 현대적 인테리어", "모던,세련,깔끔,미니멀,심플,현대적,화이트"),
    RETRO     ("레트로",       "복고풍 또는 빈티지 스타일의 인테리어", "레트로,빈티지,복고,오래된,감성,다방,앤틱"),
    NATURE    ("자연",         "식물이나 자연 소재를 활용한 초록 분위기", "자연,식물,초록,나무,그린,식물원,가드닝"),
    INDUSTRIAL("인더스트리얼", "벽돌·철제 등을 활용한 공장풍 인테리어", "인더스트리얼,공장,벽돌,철제,노출,빈티지"),
    CLASSIC   ("클래식",       "고전적이고 우아한 유럽풍 인테리어", "클래식,우아,고급,유럽,앤틱,전통");

    private final String label;
    private final String description;
    private final String keywords;

    MoodTag(String label, String description, String keywords) {
        this.label = label;
        this.description = description;
        this.keywords = keywords;
    }

    @Override public String getLabel()       { return label; }
    @Override public String getDescription() { return description; }
    @Override public String getKeywords()    { return keywords; }
    @Override public String getCategory()      { return "MOOD"; }
    @Override public String getCategoryLabel() { return "분위기"; }
}
