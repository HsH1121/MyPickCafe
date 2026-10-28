// src/main/java/com/example/MyPickCafe/service/FileStorageService.java
package com.example.MyPickCafe.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

// 저장소 구현은 file.storage 값으로 고른다: local(기본) → LocalFileStorageService, s3 → S3FileStorageService
public interface FileStorageService {

    // 저장 후 DB에 넣을 URL과 메타데이터를 돌려준다
    StoredFile store(MultipartFile file, String subDir) throws IOException;

    void delete(String url);

    // DB 컬럼 값 ↔ 화면용 URL 변환(StoredFileUrlConverter 가 사용). 기본은 그대로 저장한다.
    default String toStoredValue(String url) {
        return url;
    }

    default String toUrl(String storedValue) {
        return storedValue;
    }

    default String save(MultipartFile file, String subDir) {
        try {
            return store(file, subDir).url();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record StoredFile(String storedName, String originalName, String url,
                      String contentType, long sizeBytes) {}
}
