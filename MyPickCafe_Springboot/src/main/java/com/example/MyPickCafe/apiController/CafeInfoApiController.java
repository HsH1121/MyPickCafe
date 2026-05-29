package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafeInfo;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.CafeInfoService;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.support.NotFoundException;
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
    public ResponseEntity<CafeInfo> byCafe(@PathVariable Long cafeId) {
        return service.findByCafeId(cafeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/upsert/{cafeId}")
    @PreAuthorize("isAuthenticated()")
    public CafeInfo upsert(@PathVariable Long cafeId,
                           @RequestBody @Valid CafeInfo body,
                           Authentication auth) {
        Cafe cafe = cafeService.findById(cafeId);
        ensureOwner(auth, cafe);

        body.setCafe(cafe);
        return service.upsertByCafeId(cafeId, body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CafeInfo update(@PathVariable Long id,
                           @RequestBody @Valid CafeInfo body,
                           Authentication auth) {
        CafeInfo cur = service.findById(id);
        ensureOwner(auth, cur.getCafe());

        body.setId(id);
        body.setCafe(cur.getCafe());
        return service.update(id, body);
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
