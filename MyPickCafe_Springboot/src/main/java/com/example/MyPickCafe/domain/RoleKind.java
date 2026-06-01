package com.example.MyPickCafe.domain;

public enum RoleKind {
    ADMIN, MEMBER, CAFEOWNER;

    public String label() {
        return switch (this) {
            case ADMIN -> "관리자";
            case MEMBER -> "일반";
            case CAFEOWNER -> "카페 사장";
        };
    }
}

