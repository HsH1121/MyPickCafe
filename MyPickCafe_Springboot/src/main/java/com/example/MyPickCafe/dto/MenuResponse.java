package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Menu;
import com.example.MyPickCafe.entity.MenuCategory;

/**
 * 메뉴 조회 응답 DTO.
 *
 * <p>Menu 엔티티를 그대로 직렬화하면 {@code cafe} → {@code owner}(Member)까지
 * 연쇄적으로 따라가 점주 개인정보가 노출된다. 식별자만 남긴다.
 */
public record MenuResponse(
        Long id,
        Long cafeId,
        Long categoryId,
        String categoryName,
        String name,
        int price,
        boolean isNew,
        boolean isRecommended,
        String photo
) {
    public static MenuResponse from(Menu m) {
        MenuCategory category = m.getMenuCategory();
        return new MenuResponse(
                m.getId(),
                m.getCafe() == null ? null : m.getCafe().getId(),
                category == null ? null : category.getMenuCategoryId(),
                category == null ? null : category.getCategory(),
                m.getName(),
                m.getPrice(),
                m.isNew(),
                m.isRecommended(),
                m.getPhoto()
        );
    }
}
