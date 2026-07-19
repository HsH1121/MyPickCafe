package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.dto.CafeCreateRequest;
import com.example.MyPickCafe.dto.CafeResponse;
import com.example.MyPickCafe.dto.CafeUpdateRequest;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * 카페 REST API.
 *
 * <p>조회는 공개, 등록/수정/삭제는 점주 또는 관리자만 가능하다.
 * 응답은 항상 {@link CafeResponse}로 내려 점주 개인정보와
 * 사업자 증빙 문서 경로가 노출되지 않도록 한다.
 */
@RestController
@RequestMapping("/api/cafes")
@RequiredArgsConstructor
@Tag(name = "Cafe", description = "카페 조회 및 관리")
public class CafeApiController {

    private final CafeService cafeService;
    private final MemberService memberService;

    @GetMapping
    @Operation(summary = "카페 전체 조회", description = "인증 없이 호출할 수 있다.")
    public List<CafeResponse> getAll() {
        return cafeService.findAllForApi();
    }

    @GetMapping("/{id}")
    @Operation(summary = "카페 단건 조회")
    @ApiResponse(responseCode = "404", description = "해당 ID의 카페가 없음")
    public CafeResponse getOne(@PathVariable Long id) {
        return cafeService.findByIdForApi(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CAFEOWNER', 'ADMIN')")
    @Operation(summary = "카페 등록",
            description = "소유자는 인증 주체에서 결정되며, 승인 상태는 항상 PENDING으로 시작한다.")
    public ResponseEntity<CafeResponse> create(@RequestBody @Valid CafeCreateRequest body,
                                               Authentication auth,
                                               UriComponentsBuilder uriBuilder) {
        Long ownerId = memberService.findByEmail(auth.getName()).getId();
        CafeResponse saved = cafeService.createFromRequest(body, ownerId);
        URI location = uriBuilder.path("/api/cafes/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAFEOWNER', 'ADMIN')")
    public CafeResponse update(@PathVariable Long id, @RequestBody @Valid CafeUpdateRequest body) {
        return cafeService.updateFromRequest(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAFEOWNER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cafeService.delete(id);
    }
}
