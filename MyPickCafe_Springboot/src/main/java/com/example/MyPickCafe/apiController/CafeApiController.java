package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.service.CafeService;
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
@RequestMapping("/api/cafes")
@RequiredArgsConstructor
public class CafeApiController {

    private final CafeService cafeService;

    @GetMapping
    public List<Cafe> getAll() {
        return cafeService.findAll();
    }

    @GetMapping("/{id}")
    public Cafe getOne(@PathVariable Long id) {
        return cafeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Cafe> create(@RequestBody @Valid Cafe body, UriComponentsBuilder uriBuilder) {
        Cafe saved = cafeService.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/cafes/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Cafe update(@PathVariable Long id, @RequestBody @Valid Cafe body) {
        return cafeService.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cafeService.delete(id);
    }
}
