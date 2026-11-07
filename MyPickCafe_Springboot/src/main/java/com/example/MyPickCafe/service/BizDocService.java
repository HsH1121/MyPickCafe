package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.util.StoredFiles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 카페 사업자 증빙 문서(사업자등록증 등).
 *
 * <p>공개 업로드 폴더가 아니라 {@link PrivateFileStorage} 에 두고, 관리자 엔드포인트
 * ({@code GET /admin/cafes/{cafeId}/bizdoc})가 권한을 확인한 뒤에만 내려준다.
 * {@code cafe.biz_doc} 컬럼에는 비공개 저장소 키({@code bizdocs/{cafeId}/{uuid}.pdf})만 둔다.
 *
 * <p>예전에는 공개 폴더({@code /uploads/cafes/{cafeId}/docs/})에 저장해 URL 만 알면 로그인 없이 받을 수 있었다.
 * 그때 올라간 파일은 {@link #migrateLegacy()} 로 옮긴다.
 */
@Service
@RequiredArgsConstructor
public class BizDocService {

    private static final String KEY_PREFIX = "bizdocs/";

    // 브라우저에서 바로 열어도 스크립트가 실행되지 않는 형식만 화면에 띄운다.
    // SVG·HTML 등 나머지는 관리자 화면(같은 출처)에서 실행되지 않게 내려받기로만 준다.
    private static final Map<String, MediaType> INLINE_TYPES = Map.of(
            "pdf", MediaType.APPLICATION_PDF,
            "png", MediaType.IMAGE_PNG,
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "gif", MediaType.IMAGE_GIF,
            "webp", MediaType.parseMediaType("image/webp")
    );

    private final PrivateFileStorage privateStorage;
    private final FileStorageService publicStorage;
    private final CafeRepository cafeRepository;

    /** 증빙 파일을 비공개 저장소에 넣고 cafe.bizDoc 에 저장할 키를 돌려준다. */
    public String store(Long cafeId, MultipartFile file) throws IOException {
        String key = newKey(cafeId, StoredFiles.safeExtension(file.getOriginalFilename()));
        privateStorage.put(key, file.getBytes(), file.getContentType());
        return key;
    }

    /**
     * 카페의 증빙을 읽는다. 값이 이 카페의 비공개 키가 아니면 빈 값이다.
     * (예전 공개 경로, 수정 전 등록 폼으로 들어갔을 수 있는 외부 URL·서버 경로·다른 카페의 키)
     */
    public Optional<BizDoc> load(Cafe cafe) throws IOException {
        String key = cafe.getBizDoc();
        if (!isOwnKey(cafe.getId(), key)) return Optional.empty();

        Optional<byte[]> content = privateStorage.get(key);
        if (content.isEmpty()) return Optional.empty();

        String ext = key.contains(".") ? key.substring(key.lastIndexOf('.') + 1) : "";
        MediaType inlineType = INLINE_TYPES.get(ext);
        String fileName = "bizdoc-" + cafe.getId() + (ext.isEmpty() ? "" : "." + ext);
        return Optional.of(new BizDoc(content.get(), fileName,
                inlineType != null ? inlineType : MediaType.APPLICATION_OCTET_STREAM,
                inlineType != null));
    }

    /**
     * 예전에 공개 업로드 폴더(로컬 {@code /uploads/cafes/{cafeId}/docs/}, S3 {@code cafes/{cafeId}/docs/})에
     * 저장된 증빙을 비공개 저장소로 옮기고 공개 파일은 지운다. 여러 번 실행해도 된다.
     *
     * <p>자기 카페의 docs 폴더를 가리키는 값만 옮긴다. 수정 전 등록 폼으로 들어갔을 수 있는
     * 외부 URL·다른 카페의 사진·임의 경로는 건드리지 않고 {@code skipped} 로 센다
     * (옮기면 남의 공개 사진이 지워진다). 파일이 이미 없으면 {@code missing}.
     */
    @Transactional
    public MigrationResult migrateLegacy() throws IOException {
        int migrated = 0;
        int skipped = 0;
        int missing = 0;
        for (Cafe cafe : cafeRepository.findByBizDocIsNotNull()) {
            String value = cafe.getBizDoc();
            if (value.isBlank() || isOwnKey(cafe.getId(), value)) continue;

            Optional<String> legacyUrl = legacyPublicUrl(cafe.getId(), value);
            if (legacyUrl.isEmpty()) {
                skipped++;
                continue;
            }
            Optional<byte[]> content = publicStorage.read(legacyUrl.get());
            if (content.isEmpty()) {
                missing++;
                continue;
            }

            String key = newKey(cafe.getId(), StoredFiles.safeExtension(value));
            privateStorage.put(key, content.get(), null);
            cafe.setBizDoc(key);
            // DB 에 새 키가 반영된 뒤에 공개 파일을 지운다
            cafeRepository.saveAndFlush(cafe);
            publicStorage.delete(legacyUrl.get());
            migrated++;
        }
        return new MigrationResult(migrated, skipped, missing);
    }

    private static String newKey(Long cafeId, String ext) {
        return KEY_PREFIX + cafeId + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private static boolean isOwnKey(Long cafeId, String value) {
        return value != null
                && value.matches(KEY_PREFIX + cafeId + "/[0-9a-f]{32}(\\.[a-z0-9]{1,10})?");
    }

    // 자기 카페 docs 폴더 바로 아래 파일 하나를 가리키는 예전 공개 값이면 그 공개 URL 을 돌려준다
    private Optional<String> legacyPublicUrl(Long cafeId, String value) {
        String relative;
        if (value.startsWith("/uploads/")) {
            relative = value.substring("/uploads/".length());
        } else if (value.startsWith("/files/")) {
            relative = value.substring("/files/".length());
        } else if (!value.startsWith("/") && !value.contains("://") && !value.contains(":\\")) {
            relative = value; // S3 시절 DB 에 들어간 객체 키
        } else {
            return Optional.empty();
        }

        String docsDir = "cafes/" + cafeId + "/docs/";
        if (!relative.startsWith(docsDir)) return Optional.empty();
        String name = relative.substring(docsDir.length());
        if (!name.matches("[A-Za-z0-9_-][A-Za-z0-9._-]*")) return Optional.empty();

        return Optional.of(value.startsWith("/") ? value : publicStorage.toUrl(value));
    }

    public record BizDoc(byte[] content, String fileName, MediaType mediaType, boolean inline) {}

    public record MigrationResult(int migrated, int skipped, int missing) {}
}
