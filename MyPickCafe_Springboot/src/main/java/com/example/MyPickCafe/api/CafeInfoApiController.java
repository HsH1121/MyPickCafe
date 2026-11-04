package com.example.MyPickCafe.api;

import com.example.MyPickCafe.dto.CafeInfoRequest;
import com.example.MyPickCafe.dto.CafeInfoResponse;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.CafeInfoService;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cafe-infos")
public class CafeInfoApiController {

    private final CafeInfoService service;
    private final CafeService cafeService;
    private final MemberService memberService;

    @GetMapping("/by-cafe/{cafeId}")
    public ResponseEntity<CafeInfoResponse> byCafe(@PathVariable Long cafeId, Authentication auth) {
        cafeService.findVisibleById(cafeId, auth); // 승인 전 카페는 점주·관리자만(아니면 404)
        return service.findByCafeId(cafeId)
                .map(CafeInfoResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 요청은 DTO 로 받는다. 엔티티로 받으면 본문의 id 가 바인딩되어 다른 카페의 영업정보를 덮어쓸 수 있었다.
    @PostMapping("/upsert/{cafeId}")
    @PreAuthorize("isAuthenticated()")
    public CafeInfoResponse upsert(@PathVariable Long cafeId,
                                   @RequestBody @Valid CafeInfoRequest body,
                                   Authentication auth) {
        Cafe cafe = cafeService.findById(cafeId);
        ensureOwner(auth, cafe);
        return CafeInfoResponse.from(service.upsertByCafe(cafe, body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CafeInfoResponse update(@PathVariable Long id,
                                   @RequestBody @Valid CafeInfoRequest body,
                                   Authentication auth) {
        CafeInfo cur = service.findById(id);
        ensureOwner(auth, cur.getCafe());
        return CafeInfoResponse.from(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable Long id, Authentication auth) {
        CafeInfo cur = service.findById(id);
        ensureOwner(auth, cur.getCafe());
        service.delete(id);
    }

    private void ensureOwner(Authentication auth, Cafe cafe) {
        if (auth == null || !auth.isAuthenticated())
            throw new org.springframework.security.access.AccessDeniedException("로그인 필요");

        Member me = memberService.findByEmailOptional(auth.getName()).orElse(null);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isOwner = me != null && cafe.getOwner() != null
                && cafe.getOwner().getId().equals(me.getId());

        if (!(isOwner || isAdmin))
            throw new org.springframework.security.access.AccessDeniedException("점주만 가능합니다.");
    }
}
