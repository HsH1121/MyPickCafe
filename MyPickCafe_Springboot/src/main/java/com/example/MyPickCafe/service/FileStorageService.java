// src/main/java/com/example/MyPickCafe/service/FileStorageService.java
package com.example.MyPickCafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

public abstract class FileStorageService {

    @Value("${file.upload-dir}")
    private String root;

    // 공통 저장 로직
    public StoredFile store(MultipartFile file, String subDir) throws IOException {
        String ext = "";
        String original = file.getOriginalFilename();
        int dot = original != null ? original.lastIndexOf('.') : -1;
        if (dot > -1) ext = original.substring(dot);
        String stored = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = Paths.get(root, subDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        Path target = dir.resolve(stored);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String url = "/uploads/" + subDir.replace('\\', '/').replaceAll("^/+", "") + "/" + stored;
        return new StoredFile(stored, original, url, file.getContentType(), file.getSize());
    }

    public abstract String save(MultipartFile file, String subDir);

    public abstract void delete(String url);

    public record StoredFile(String storedName, String originalName, String url,
                             String contentType, long sizeBytes) {}
}
