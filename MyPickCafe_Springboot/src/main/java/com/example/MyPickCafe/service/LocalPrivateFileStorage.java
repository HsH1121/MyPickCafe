package com.example.MyPickCafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

// 서버 디스크의 file.private-dir 에 저장한다. 이 폴더는 WebConfig 의 /uploads/** 처럼 URL 로 열어 두지 않는다.
@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "local", matchIfMissing = true)
public class LocalPrivateFileStorage implements PrivateFileStorage {

    private final Path root;

    public LocalPrivateFileStorage(@Value("${file.private-dir}") String privateDir) {
        this.root = Paths.get(privateDir).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, byte[] content, String contentType) throws IOException {
        Path path = resolve(key);
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }

    @Override
    public Optional<byte[]> get(String key) throws IOException {
        Path path = resolve(key);
        return Files.isRegularFile(path) ? Optional.of(Files.readAllBytes(path)) : Optional.empty();
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
        }
    }

    // 키가 저장 폴더 밖(../, 절대 경로)을 가리키면 거부한다
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (key.isBlank() || path.equals(root) || !path.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 파일 키입니다: " + key);
        }
        return path;
    }
}
