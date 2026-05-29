package com.example.MyPickCafe.dto;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.MenuCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryForm {

    private Long categoryId;
    private Cafe cafe;
    private String category;

    public MenuCategory toEntity() {
        MenuCategory m = new MenuCategory();
        m.setMenuCategoryId(categoryId);
        m.setCafe(cafe);
        m.setCategory(category);
        return m;
    }
}
