package com.example.MyPickCafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// LocalFileStorageService
@Service
public class LocalFileStorageService extends FileStorageService {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public String save(MultipartFile file, String subDir) {
        try {
            return store(file, subDir).url(); // 부모가 "/uploads/..."를 반환
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null) return;
        String prefix = url.startsWith("/files/") ? "/files/" : "/uploads/";
        if (!url.startsWith(prefix)) return;
        String rel = url.substring(prefix.length());
        Path path = Paths.get(uploadDir, rel).toAbsolutePath().normalize();
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }
}
