// src/main/java/com/example/MyPickCafe/service/FileStorageService.java
package com.example.MyPickCafe.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// 저장소 구현은 file.storage 값으로 고른다: local(기본) → LocalFileStorageService, s3 → S3FileStorageService
public abstract class FileStorageService {

    // 저장 후 DB에 넣을 URL과 메타데이터를 돌려준다
    public abstract StoredFile store(MultipartFile file, String subDir) throws IOException;

    public abstract void delete(String url);

    public String save(MultipartFile file, String subDir) {
        try {
            return store(file, subDir).url();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // 원본 확장자만 살리고 이름은 UUID로 바꾼다(충돌·한글 파일명 문제 방지)
    protected static String newStoredName(MultipartFile file) {
        String original = file.getOriginalFilename();
        int dot = original != null ? original.lastIndexOf('.') : -1;
        String ext = dot > -1 ? original.substring(dot) : "";
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    // "\cafes\1/" → "cafes/1"
    protected static String normalizeSubDir(String subDir) {
        return subDir.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    // 로컬에 저장된 /uploads/..., /files/... 파일을 지운다. 업로드 폴더 밖을 가리키면 무시한다.
    protected static void deleteLocalFile(String uploadDir, String url) {
        if (url == null) return;
        String prefix = url.startsWith("/files/") ? "/files/" : "/uploads/";
        if (!url.startsWith(prefix)) return;
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(url.substring(prefix.length())).normalize();
        if (!path.startsWith(root)) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }

    public record StoredFile(String storedName, String originalName, String url,
                             String contentType, long sizeBytes) {}
}
