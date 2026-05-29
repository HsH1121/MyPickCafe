package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.UserNeeds;
import com.example.MyPickCafe.service.NeedsService;
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
@RequestMapping("/api/needs")
@RequiredArgsConstructor
public class NeedsApiController {

    private final NeedsService service;

    @GetMapping
    public List<UserNeeds> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserNeeds getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<UserNeeds> create(@RequestBody @Valid UserNeeds body, UriComponentsBuilder uriBuilder) {
        UserNeeds saved = service.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/needs/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public UserNeeds update(@PathVariable Long id, @RequestBody @Valid UserNeeds body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
