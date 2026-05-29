package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.CafeTag;
import com.example.MyPickCafe.service.CafeTagService;
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
@RequestMapping("/api/cafe-tags")
@RequiredArgsConstructor
public class CafeTagApiController {

    private final CafeTagService service;

    @GetMapping
    public List<CafeTag> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CafeTag getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<CafeTag> create(@RequestBody @Valid CafeTag body, UriComponentsBuilder uriBuilder) {
        CafeTag saved = service.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/cafe-tags/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public CafeTag update(@PathVariable Long id, @RequestBody @Valid CafeTag body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
