package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<MenuCategory, Long> {
}