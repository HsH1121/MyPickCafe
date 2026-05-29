package com.example.MyPickCafe.repository;

import com.example.MyPickCafe.entity.TagDictionary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagDictionaryRepository extends JpaRepository<TagDictionary, Long> {
    List<TagDictionary> findByTagCategory(String tagCategory);
}
