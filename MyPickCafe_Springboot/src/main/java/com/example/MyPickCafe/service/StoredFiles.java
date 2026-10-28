package com.example.MyPickCafe.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// FileStorageService 구현들이 함께 쓰는 파일명·경로 처리
final class StoredFiles {

    private StoredFiles() {}

    // 원본 확장자만 살리고 이름은 UUID로 바꾼다(충돌·한글 파일명 문제 방지)
    static String newStoredName(MultipartFile file) {
        String original = file.getOriginalFilename();
        int dot = original != null ? original.lastIndexOf('.') : -1;
        String ext = dot > -1 ? original.substring(dot) : "";
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    // "\cafes\1/" → "cafes/1"
    static String normalizeSubDir(String subDir) {
        return subDir.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    // 로컬에 저장된 /uploads/..., /files/... 파일을 지운다. 업로드 폴더 밖을 가리키면 무시한다.
    static void deleteLocalFile(String uploadDir, String url) {
        if (url == null) return;
        String prefix = url.startsWith("/files/") ? "/files/" : "/uploads/";
        if (!url.startsWith(prefix)) return;
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(url.substring(prefix.length())).normalize();
        if (!path.startsWith(root)) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }
}
