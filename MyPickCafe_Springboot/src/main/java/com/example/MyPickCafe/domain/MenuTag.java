package com.example.MyPickCafe.domain;

public enum MenuTag implements TagEnum {
    AMERICANO("아메리카노", "아메리카노가 특색 있거나 맛이 좋다고 언급됨", "아메리카노,블랙커피,에스프레소,커피,아아,아이스아메리카노"),
    LATTE    ("라떼",      "라떼 계열 음료(카페라떼·플랫화이트 등)가 맛있음", "라떼,카페라떼,플랫화이트,우유,밀크,크리미"),
    COLDBREW ("콜드브루",  "콜드브루 또는 더치커피가 특색 있거나 맛이 좋음", "콜드브루,더치,차가운커피,콜드,찬커피,질소커피"),
    BAKERY   ("베이커리",  "크루아상·파운드케이크 등 빵 종류가 맛있음", "베이커리,빵,크루아상,파운드,스콘,마들렌,브레드"),
    CAKE     ("케이크",    "케이크 종류가 다양하거나 특별히 맛있음", "케이크,조각케이크,크림케이크,치즈케이크,레이어케이크,바스크"),
    ADE      ("에이드",    "에이드나 청량음료류가 맛있거나 특색 있음", "에이드,레몬에이드,유자에이드,자몽에이드,청량,스파클링"),
    DESSERT  ("디저트",    "다양한 디저트 메뉴가 있거나 특별히 맛있음", "디저트,티라미수,마카롱,와플,빙수,달콤,스위트");

    private final String label;
    private final String description;
    private final String keywords;

    MenuTag(String label, String description, String keywords) {
        this.label = label;
        this.description = description;
        this.keywords = keywords;
    }

    @Override public String getLabel()       { return label; }
    @Override public String getDescription() { return description; }
    @Override public String getKeywords()    { return keywords; }
    @Override public String getCategory()      { return "MENU"; }
    @Override public String getCategoryLabel() { return "메뉴"; }
}
