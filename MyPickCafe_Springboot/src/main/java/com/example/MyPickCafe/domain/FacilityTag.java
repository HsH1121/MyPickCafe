package com.example.MyPickCafe.domain;

public enum FacilityTag implements TagEnum {
    PLUG    ("콘센트",   "카페에 콘센트·전원이 있어 노트북 및 스마트기기 충전 가능", "콘센트,충전,전원,노트북,작업,플러그"),
    TERRACE ("테라스",   "야외 테라스 또는 루프탑 공간이 있어 야외에서 음료를 즐길 수 있음", "테라스,야외,루프탑,실외,야외석,바깥"),
    PET     ("반려동물", "반려동물 동반 입장 가능한 펫프렌들리 카페", "반려동물,강아지,고양이,펫,애견,동물"),
    PARKING ("주차",    "카페 전용 주차장 또는 근처 주차 가능 공간 제공", "주차,주차장,주차공간,차,주차가능"),
    WIFI    ("와이파이", "고속 무선 인터넷 와이파이 서비스 제공", "와이파이,wifi,인터넷,무선,네트워크,랜");

    private final String label;
    private final String description;
    private final String keywords;

    FacilityTag(String label, String description, String keywords) {
        this.label = label;
        this.description = description;
        this.keywords = keywords;
    }

    @Override public String getLabel()       { return label; }
    @Override public String getDescription() { return description; }
    @Override public String getKeywords()    { return keywords; }
    @Override public String getCategory()      { return "FACILITY"; }
    @Override public String getCategoryLabel() { return "시설"; }
}
