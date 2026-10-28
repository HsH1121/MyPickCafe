package com.example.MyPickCafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

// 서버 디스크(file.upload-dir)에 저장하고 WebConfig 의 /uploads/** 로 서빙한다. 로컬 개발 기본값.
@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public StoredFile store(MultipartFile file, String subDir) throws IOException {
        String dirName = StoredFiles.normalizeSubDir(subDir);
        String stored = StoredFiles.newStoredName(file);

        Path dir = Paths.get(uploadDir, dirName).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        }

        String url = "/uploads/" + dirName + "/" + stored;
        return new StoredFile(stored, file.getOriginalFilename(), url, file.getContentType(), file.getSize());
    }

    @Override
    public void delete(String url) {
        StoredFiles.deleteLocalFile(uploadDir, url);
    }
}
