package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.Review;
import com.example.MyPickCafe.service.ReviewService;
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
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService service;

    @GetMapping
    public List<Review> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Review getOne(@PathVariable Long id) {
        return service.findById(id);
    }

//    @PostMapping
//    public ResponseEntity<Review> create(@RequestBody @Valid Review body, UriComponentsBuilder uriBuilder) {
//        Review saved = service.create(body);
//        Object id = EntityIdUtil.getId(saved);
//        URI location = uriBuilder.path("/api/reviews/{id}").buildAndExpand(id).toUri();
//        return ResponseEntity.created(location).body(saved);
//    }

    @PutMapping("/{id}")
    public Review update(@PathVariable Long id, @RequestBody @Valid Review body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
