package com.example.MyPickCafe.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// FileStorageService 구현들이 함께 쓰는 파일명·경로 처리
public final class StoredFiles {

    private StoredFiles() {}

    // 원본 확장자만 살리고 이름은 UUID로 바꾼다(충돌·한글 파일명 문제 방지)
    public static String newStoredName(MultipartFile file) {
        String original = file.getOriginalFilename();
        int dot = original != null ? original.lastIndexOf('.') : -1;
        String ext = dot > -1 ? original.substring(dot) : "";
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    // 확장자가 영문·숫자 1~10자일 때만 소문자로 돌려준다("a.PDF" → ".pdf"). 그 밖에는 빈 문자열.
    // 파일명에 "/"나 ".." 가 섞여 들어와도 저장 경로·키가 바뀌지 않게 한다.
    public static String safeExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = fileName.substring(dot + 1);
        return ext.matches("[A-Za-z0-9]{1,10}") ? "." + ext.toLowerCase(Locale.ROOT) : "";
    }

    // "\cafes\1/" → "cafes/1"
    public static String normalizeSubDir(String subDir) {
        return subDir.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    // /uploads/..., /files/... 값을 업로드 폴더 안의 실제 경로로 바꾼다. 다른 형식이거나 폴더 밖을 가리키면 빈 값.
    public static Optional<Path> resolveLocalFile(String uploadDir, String url) {
        if (url == null) return Optional.empty();
        String prefix = url.startsWith("/files/") ? "/files/" : "/uploads/";
        if (!url.startsWith(prefix)) return Optional.empty();
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(url.substring(prefix.length())).normalize();
        return path.startsWith(root) ? Optional.of(path) : Optional.empty();
    }

    // 로컬에 저장된 /uploads/..., /files/... 파일을 지운다. 업로드 폴더 밖을 가리키면 무시한다.
    public static void deleteLocalFile(String uploadDir, String url) {
        resolveLocalFile(uploadDir, url).ifPresent(path -> {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        });
    }

    // 로컬에 저장된 /uploads/..., /files/... 파일 내용을 읽는다. 없거나 업로드 폴더 밖이면 빈 값.
    public static Optional<byte[]> readLocalFile(String uploadDir, String url) throws IOException {
        Optional<Path> path = resolveLocalFile(uploadDir, url);
        if (path.isEmpty() || !Files.isRegularFile(path.get())) return Optional.empty();
        return Optional.of(Files.readAllBytes(path.get()));
    }
}
