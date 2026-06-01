package com.example.MyPickCafe.domain;

public enum MenuTag implements TagEnum {
    AMERICANO("아메리카노"),
    LATTE    ("라떼"),
    COLDBREW ("콜드브루"),
    BAKERY   ("베이커리"),
    CAKE     ("케이크"),
    ADE      ("에이드"),
    DESSERT  ("디저트");

    private final String label;

    MenuTag(String label) {
        this.label = label;
    }

    @Override public String getLabel()         { return label; }
    @Override public String getCategory()      { return "MENU"; }
    @Override public String getCategoryLabel() { return "메뉴"; }
}
