package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.entity.Menu;
import com.example.MyPickCafe.service.FileStorageService;
import com.example.MyPickCafe.service.MenuService;
import com.example.MyPickCafe.support.EntityIdUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuApiController {

    private final MenuService service;
    private final FileStorageService storage;

    @GetMapping
    public List<Menu> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Menu getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Menu> create(@RequestBody @Valid Menu body, UriComponentsBuilder uriBuilder) {
        Menu saved = service.create(body);
        Object id = EntityIdUtil.getId(saved);
        URI location = uriBuilder.path("/api/menus/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Menu update(@PathVariable Long id, @RequestBody @Valid Menu body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/by-cafe/{cafeId}")
    public List<Menu> listByCafe(@PathVariable Long cafeId) {
        return service.findByCafeId(cafeId);
    }

    @PostMapping(value = "/{menuId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, String> uploadPhoto(@PathVariable Long menuId,
                                           @RequestParam("file") MultipartFile file) throws Exception {
        Menu menu = service.findById(menuId);
        // 원하는 경로 규칙으로 저장
        String url = storage.save(file, "menus/" + menu.getCafe().getId() + "/" + menuId);
        menu.setPhoto(url);
        // JPA dirty checking 으로 update, 혹은 menuService.update(menuId, menu)
        return Map.of("url", url);
    }
}
