// src/main/java/com/example/MyPickCafe/controller/CafePhotoController.java
package com.example.MyPickCafe.controller;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.security.CafeOwnershipGuard;
import com.example.MyPickCafe.service.CafePhotoService;
import com.example.MyPickCafe.service.CafeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cafes")
public class CafePhotoController {

    private final CafeService cafeService;
    private final CafePhotoService cafePhotoService;
    private final CafeOwnershipGuard cafeOwnershipGuard;

    @PostMapping("/{cafeId}/photos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CafePhoto>> upload(@PathVariable Long cafeId,
                                                  @RequestParam("files") List<MultipartFile> files,
                                                  @RequestParam(value = "setMain", defaultValue = "false") boolean setMain,
                                                  Authentication auth) throws Exception {
        Cafe cafe = cafeService.findById(cafeId);
        ensureOwner(auth, cafe);
        return ResponseEntity.ok(cafePhotoService.upload(cafe, files, setMain));
    }

    @PatchMapping("/photos/{photoId}/main")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CafePhoto>> setMain(@PathVariable Long photoId, Authentication auth) {
        CafePhoto target = cafePhotoService.findById(photoId);
        ensureOwner(auth, target.getCafe());
        return ResponseEntity.ok(cafePhotoService.setMainPhoto(photoId));
    }

    @GetMapping("/{cafeId}/photos")
    public List<CafePhoto> list(@PathVariable Long cafeId) {
        return cafePhotoService.list(cafeId);
    }

    @DeleteMapping("/photos/{photoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CafePhoto>> delete(@PathVariable Long photoId, Authentication auth) {
        CafePhoto target = cafePhotoService.findById(photoId);
        ensureOwner(auth, target.getCafe());
        return ResponseEntity.ok(cafePhotoService.deletePhoto(photoId));
    }

    private void ensureOwner(Authentication auth, Cafe cafe) {
        cafeOwnershipGuard.ensureOwner(auth, cafe);
    }
}
