package com.example.MyPickCafe.domain;

public enum MenuTag {
    AMERICANO("아메리카노"),
    LATTE("라떼"),
    COLDBREW("콜드브루"),
    BAKERY("베이커리"),
    CAKE("케이크"),
    ADE("에이드"),
    DESSERT("디저트");

    private final String label;

    MenuTag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
