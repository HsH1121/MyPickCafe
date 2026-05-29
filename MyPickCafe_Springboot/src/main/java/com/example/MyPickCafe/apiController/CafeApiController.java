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

    private final CafeService service;

    @GetMapping
    public List<Cafe> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Cafe getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Cafe> create(@RequestBody @Valid Cafe body, UriComponentsBuilder uriBuilder) {
        Cafe saved = service.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/cafes/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Cafe update(@PathVariable Long id, @RequestBody @Valid Cafe body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
