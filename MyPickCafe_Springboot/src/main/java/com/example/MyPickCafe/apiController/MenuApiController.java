package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.dto.MenuResponse;
import com.example.MyPickCafe.dto.MenuUpdateRequest;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Menu;
import com.example.MyPickCafe.security.CafeOwnershipGuard;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.FileStorageService;
import com.example.MyPickCafe.service.MenuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 메뉴 REST API.
 *
 * <p>조회는 공개, 등록/수정/삭제는 <b>해당 카페의</b> 점주 또는 관리자만 가능하다.
 * URL 기반 인가(SecurityConfig)는 "CAFEOWNER인가"까지만 판단하므로,
 * 리소스 단위 소유권은 {@link CafeOwnershipGuard}로 별도 검증한다.
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuApiController {

    private final MenuService service;
    private final CafeService cafeService;
    private final FileStorageService storage;
    private final CafeOwnershipGuard ownershipGuard;

    @GetMapping("/{id}")
    public MenuResponse getOne(@PathVariable Long id) {
        return service.findByIdForApi(id);
    }

    @GetMapping("/by-cafe/{cafeId}")
    public List<MenuResponse> listByCafe(@PathVariable Long cafeId) {
        return service.findByCafeIdForApi(cafeId);
    }

    /**
     * 메뉴 등록.
     *
     * <p>프론트엔드가 사진 파일을 함께 보내므로 multipart/form-data로 받는다.
     * (기존에는 {@code @RequestBody Menu}로 선언돼 있어 JSON만 받을 수 있었고,
     *  실제 화면에서 보내는 FormData 요청은 415로 실패하고 있었다.)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuResponse> create(@RequestParam Long cafeId,
                                               @RequestParam @NotBlank String name,
                                               @RequestParam @Min(0) int price,
                                               @RequestParam(defaultValue = "false") boolean isNew,
                                               @RequestParam(defaultValue = "false") boolean isRecommended,
                                               @RequestParam(value = "file", required = false) MultipartFile file,
                                               Authentication auth,
                                               UriComponentsBuilder uriBuilder) throws Exception {
        Cafe cafe = cafeService.findById(cafeId);
        ownershipGuard.ensureOwner(auth, cafe);

        String photoUrl = null;
        if (file != null && !file.isEmpty()) {
            photoUrl = storage.save(file, "menus/" + cafeId);
        }

        MenuResponse saved = service.createMenu(cafe, name, price, isNew, isRecommended, photoUrl);
        URI location = uriBuilder.path("/api/menus/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public MenuResponse update(@PathVariable Long id,
                               @RequestBody @Valid MenuUpdateRequest body,
                               Authentication auth) {
        Menu menu = service.findById(id);
        ownershipGuard.ensureOwner(auth, menu.getCafe());
        return service.updateMenu(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        Menu menu = service.findById(id);
        ownershipGuard.ensureOwner(auth, menu.getCafe());
        service.delete(id);
    }

    @PostMapping(value = "/{menuId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadPhoto(@PathVariable Long menuId,
                                           @RequestParam("file") MultipartFile file,
                                           Authentication auth) throws Exception {
        Menu menu = service.findById(menuId);
        ownershipGuard.ensureOwner(auth, menu.getCafe());

        String url = storage.save(file, "menus/" + menu.getCafe().getId() + "/" + menuId);
        service.updatePhoto(menuId, url);
        return Map.of("url", url);
    }
}
