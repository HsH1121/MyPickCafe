package com.example.MyPickCafe.service;

import java.io.IOException;
import java.util.Optional;

/**
 * 공개하면 안 되는 파일(사업자 증빙 등)의 저장소.
 * FileStorageService 와 달리 URL 로 서빙하지 않는다. 앱이 권한을 확인한 뒤 내용을 읽어 직접 내려준다.
 *
 * <p>키는 "bizdocs/12/&lt;uuid&gt;.pdf" 같은 상대 경로다. 구현은 file.storage 값으로 고른다:
 * local(기본) → {@link LocalPrivateFileStorage}(file.private-dir 폴더),
 * s3 → {@link S3PrivateFileStorage}(같은 버킷의 private/ 아래).
 */
public interface PrivateFileStorage {

    void put(String key, byte[] content, String contentType) throws IOException;

    /** 파일이 없으면 빈 값. */
    Optional<byte[]> get(String key) throws IOException;

    void delete(String key);
}
