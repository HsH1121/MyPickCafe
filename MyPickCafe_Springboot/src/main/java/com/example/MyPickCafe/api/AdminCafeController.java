// AdminCafeController.java
package com.example.MyPickCafe.api;

import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.dto.CafeResponse;
import com.example.MyPickCafe.service.BizDocService;
import com.example.MyPickCafe.service.CafeService;
import com.example.MyPickCafe.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/cafes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCafeController {

    private final CafeService cafeService;
    private final ReviewService reviewService;
    private final BizDocService bizDocService;

    /** 승인된 모든 카페의 대표 태그를 리뷰 태그 기준으로 다시 계산한다. */
    @PostMapping("/tags/recalculate")
    public Map<String, Integer> recalculateTags() {
        return Map.of("cafes", reviewService.updateAllCafeTags());
    }

    @GetMapping("/pending")
    public List<CafeResponse> pending() {
        return cafeService.findByStatusForApi(CafeStatus.PENDING);
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) { cafeService.approve(id); }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id) { cafeService.reject(id); }

    /**
     * 사업자 증빙 문서를 비공개 저장소에서 읽어 직접 내려준다.
     * 공개 URL 로 리다이렉트하지 않으므로 이 엔드포인트(관리자 권한)를 거치지 않고는 받을 수 없다.
     * 브라우저·프록시에 남지 않게 캐시를 막는다.
     */
    @GetMapping("/{id}/bizdoc")
    public ResponseEntity<byte[]> bizdoc(@PathVariable Long id) throws IOException {
        return bizDocService.load(cafeService.findById(id))
                .map(doc -> ResponseEntity.ok()
                        .contentType(doc.mediaType())
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.builder(doc.inline() ? "inline" : "attachment")
                                        .filename(doc.fileName())
                                        .build()
                                        .toString())
                        .cacheControl(CacheControl.noStore())
                        .body(doc.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 예전에 공개 업로드 폴더에 저장된 사업자 증빙을 비공개 저장소로 옮긴다.
     * 배포 후 한 번 실행한다(여러 번 실행해도 된다). 결과: 옮긴 수·건너뛴 수·파일 없음 수.
     */
    @PostMapping("/bizdocs/migrate")
    public BizDocService.MigrationResult migrateBizDocs() throws IOException {
        return bizDocService.migrateLegacy();
    }
}
