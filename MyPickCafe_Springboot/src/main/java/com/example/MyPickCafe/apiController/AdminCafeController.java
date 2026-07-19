// AdminCafeController.java
package com.example.MyPickCafe.apiController;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeResponse;
import com.example.MyPickCafe.service.CafeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/cafes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCafeController {

    private final CafeService cafeService;

    @GetMapping("/pending")
    public List<CafeResponse> pending() {
        return cafeService.findByStatusForApi(CafeStatus.PENDING);
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) { cafeService.approve(id); }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id) { cafeService.reject(id); }

    @GetMapping("/{id}/bizdoc")
    public ResponseEntity<?> bizdoc(@PathVariable Long id, HttpServletRequest req) throws MalformedURLException {
        var cafe = cafeService.findById(id);
        if (cafe == null) return ResponseEntity.notFound().build();

        String doc = cafe.getBizDoc();   // 예: "/uploads/cafes/6/bizdoc.pdf" 또는 "https://..." 또는 "C:/files/bizdoc.pdf"
        if (doc == null || doc.isBlank()) return ResponseEntity.notFound().build();

        // 1) 외부 URL
        if (doc.matches("(?i)^(https?)://.*")) {
            return ResponseEntity.status(302).location(URI.create(doc)).build();
        }

        // 2) 앱이 서빙하는 웹경로(/로 시작) → 베이스 호스트 붙여 리다이렉트
        if (doc.startsWith("/")) {
            String base = req.getScheme() + "://" + req.getServerName()
                    + ((req.getServerPort() == 80 || req.getServerPort() == 443) ? "" : ":" + req.getServerPort());
            return ResponseEntity.status(302).location(URI.create(base + doc)).build();
        }

        // 3) 나머지는 로컬 파일 경로로 간주해 스트리밍
        Path path = Paths.get(doc).normalize().toAbsolutePath();
        if (!Files.exists(path) || !Files.isReadable(path)) return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(path.toUri());
        String contentType;
        try { contentType = Files.probeContentType(path); }
        catch (Exception e) { contentType = null; }
        if (contentType == null) contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

        boolean inline = contentType.startsWith("application/pdf") || contentType.startsWith("image/");
        String filename = path.getFileName().toString();
        String disposition = (inline ? "inline" : "attachment") +
                "; filename*=UTF-8''" + UriUtils.encode(filename, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }
}
