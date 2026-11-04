// src/main/java/com/example/MyPickCafe/service/FileStorageService.java
package com.example.MyPickCafe.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

// 공개 파일(카페·메뉴 사진) 저장소. 저장한 파일은 URL 만 알면 누구나 받을 수 있다.
// 사업자 증빙처럼 공개하면 안 되는 파일은 PrivateFileStorage 에 둔다.
// 저장소 구현은 file.storage 값으로 고른다: local(기본) → LocalFileStorageService, s3 → S3FileStorageService
public interface FileStorageService {

    // 저장 후 DB에 넣을 URL과 메타데이터를 돌려준다
    StoredFile store(MultipartFile file, String subDir) throws IOException;

    void delete(String url);

    // 이 저장소에 있는 파일 내용을 읽는다. 이 저장소가 관리하는 URL 이 아니거나 파일이 없으면 빈 값.
    // (예전에 공개 폴더에 저장된 사업자 증빙을 비공개 저장소로 옮길 때 쓴다)
    Optional<byte[]> read(String url) throws IOException;

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
