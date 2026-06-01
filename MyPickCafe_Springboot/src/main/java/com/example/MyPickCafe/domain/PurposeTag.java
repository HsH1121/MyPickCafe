package com.example.MyPickCafe.domain;

public enum PurposeTag implements TagEnum {
    STUDY  ("공부",   "공부하거나 혼자 집중 작업하기 좋은 환경", "공부,작업,집중,노트북,스터디,독서,조용,업무"),
    TALK   ("대화",   "친구나 지인과 이야기 나누기 좋은 환경", "대화,수다,만남,친구,모임,이야기,떠들다"),
    REST   ("휴식",   "편안하게 쉬거나 혼자 여유를 즐기기 좋은 공간", "휴식,쉬다,여유,힐링,편안,릴렉스,느긋"),
    DATE   ("데이트", "연인과 함께하기 좋은 로맨틱한 분위기", "데이트,연인,커플,로맨틱,둘이,분위기"),
    PHOTO  ("사진",   "인스타그램 사진 찍기 좋은 인테리어나 포토존이 있음", "사진,포토존,인스타,인테리어,예쁘다,감성,사진맛집"),
    MEETING("미팅",   "비즈니스 미팅이나 소규모 그룹 모임에 적합한 공간", "미팅,회의,비즈니스,단체,그룹,모임,업무");

    private final String label;
    private final String description;
    private final String keywords;

    PurposeTag(String label, String description, String keywords) {
        this.label = label;
        this.description = description;
        this.keywords = keywords;
    }

    @Override public String getLabel()       { return label; }
    @Override public String getDescription() { return description; }
    @Override public String getKeywords()    { return keywords; }
    @Override public String getCategory()      { return "PURPOSE"; }
    @Override public String getCategoryLabel() { return "목적"; }
}
