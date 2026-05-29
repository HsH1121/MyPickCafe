package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.MenuCategory;
import com.example.MyPickCafe.service.CategoryService;
import com.example.MyPickCafe.support.EntityIdUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryApiController {

    private final CategoryService service;

    @GetMapping
    public List<MenuCategory> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MenuCategory getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<MenuCategory> create(@RequestBody @Valid MenuCategory body, UriComponentsBuilder uriBuilder) {
        MenuCategory saved = service.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/categories/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public MenuCategory update(@PathVariable Long id, @RequestBody @Valid MenuCategory body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
