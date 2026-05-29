package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.MemberService;
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
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService service;

    @GetMapping
    public List<Member> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Member getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Member> create(@RequestBody @Valid Member body, UriComponentsBuilder uriBuilder) {
        Member saved = service.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/users/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Member update(@PathVariable Long id, @RequestBody @Valid Member body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
