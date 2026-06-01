// src/main/java/com/example/MyPickCafe/controller/MenuController.java
package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Menu;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.FileStorageService;
import com.example.MyPickCafe.service.MemberService;
import com.example.MyPickCafe.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;
    private final CafeService cafeService;
    private final MemberService memberService;
    private final FileStorageService storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CAFEOWNER','ADMIN')")
    public ResponseEntity<?> createWithPhoto(
            @RequestParam("cafeId") Long cafeId,
            @RequestParam("name") String name,
            @RequestParam("price") int price,
            @RequestParam(value = "isNew", defaultValue = "false") boolean isNew,
            @RequestParam(value = "isRecommended", defaultValue = "false") boolean isRecommended,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication auth
    ) {
        Cafe cafe = cafeService.findById(cafeId);
        ensureOwner(auth, cafe);

        String url = null;
        if (file != null && !file.isEmpty()) {
            url = storage.save(file, "menus");
        }

        Menu m = new Menu();
        m.setCafe(cafe);
        m.setName(name);
        m.setPrice(price);
        m.setNew(isNew);
        m.setRecommended(isRecommended);
        m.setPhoto(url);

        Menu saved = menuService.create(m);
        return ResponseEntity.ok(Map.of(
                "id",            saved.getId(),
                "name",          saved.getName(),
                "price",         saved.getPrice(),
                "isNew",         saved.isNew(),
                "isRecommended", saved.isRecommended(),
                "photo",         saved.getPhoto() != null ? saved.getPhoto() : ""
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAFEOWNER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Menu menu = menuService.findById(id);
        ensureOwner(auth, menu.getCafe());
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void ensureOwner(Authentication auth, Cafe cafe) {
        if (auth == null || !auth.isAuthenticated())
            throw new AccessDeniedException("로그인이 필요합니다.");
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return;
        Member me = memberService.findByEmailOptional(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("사용자를 찾을 수 없습니다."));
        if (cafe.getOwner() == null || !cafe.getOwner().getId().equals(me.getId()))
            throw new AccessDeniedException("카페 오너만 가능합니다.");
    }
}
