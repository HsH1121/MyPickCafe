package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.dto.MemberCreateRequest;
import com.example.MyPickCafe.dto.MemberResponse;
import com.example.MyPickCafe.dto.MemberUpdateRequest;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * 회원 관리 API (관리자 전용).
 *
 * <p>회원 정보는 개인정보이므로 클래스 단위로 ADMIN 권한을 요구한다.
 * 일반 사용자의 본인 정보 조회/수정은 {@code /api/auth/me}와
 * {@code MemberController}(마이페이지)가 담당한다.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MemberApiController {

    private final MemberService service;

    @GetMapping
    public List<MemberResponse> getAll() {
        return service.findAll().stream()
                .map(MemberResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MemberResponse getOne(@PathVariable Long id) {
        return MemberResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> create(@RequestBody @Valid MemberCreateRequest body,
                                                 UriComponentsBuilder uriBuilder) {
        Member saved = service.createMember(body);
        URI location = uriBuilder.path("/api/members/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(MemberResponse.from(saved));
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @RequestBody @Valid MemberUpdateRequest body) {
        return MemberResponse.from(service.updateMember(id, body));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
