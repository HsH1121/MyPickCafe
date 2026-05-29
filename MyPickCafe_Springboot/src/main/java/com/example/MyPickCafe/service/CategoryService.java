package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.MenuCategory;
import com.example.MyPickCafe.repository.CategoryRepository;
import com.example.MyPickCafe.support.EntityIdUtil;
import com.example.MyPickCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    @Transactional(readOnly = true)
    public List<MenuCategory> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public MenuCategory findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    @Transactional
    public MenuCategory create(MenuCategory entity) {
        EntityIdUtil.setId(entity, null);
        return repository.save(entity);
    }

    @Transactional
    public MenuCategory update(Long id, MenuCategory entity) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Category not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Category not found: " + id);
        }
        repository.deleteById(id);
    }
}
