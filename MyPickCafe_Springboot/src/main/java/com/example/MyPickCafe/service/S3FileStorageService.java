package com.example.MyPickCafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

/**
 * S3 버킷에 저장하고 DB에는 공개 URL(public-base-url + 객체 키)을 넣는다.
 * 객체 읽기는 버킷 정책(또는 CloudFront)으로 공개해야 브라우저가 이미지를 띄울 수 있다.
 * S3 전환 전에 저장된 /uploads/... 파일은 그대로 로컬에서 서빙·삭제한다.
 */
@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    // 파일명이 UUID라 같은 URL의 내용이 바뀌지 않으므로 브라우저가 오래 캐시해도 된다
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;
    private final String uploadDir;

    public S3FileStorageService(S3Client s3,
                                @Value("${file.s3.bucket:}") String bucket,
                                @Value("${file.s3.region}") String region,
                                @Value("${file.s3.public-base-url:}") String publicBaseUrl,
                                @Value("${file.upload-dir}") String uploadDir) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("file.storage=s3 이면 file.s3.bucket(S3_BUCKET)을 지정해야 합니다.");
        }
        this.s3 = s3;
        this.bucket = bucket;
        this.publicBaseUrl = (publicBaseUrl == null || publicBaseUrl.isBlank())
                ? "https://" + bucket + ".s3." + region + ".amazonaws.com"
                : publicBaseUrl.replaceAll("/+$", "");
        this.uploadDir = uploadDir;
    }

    @Override
    public StoredFile store(MultipartFile file, String subDir) throws IOException {
        String stored = StoredFiles.newStoredName(file);
        String key = StoredFiles.normalizeSubDir(subDir) + "/" + stored;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .cacheControl(CACHE_CONTROL)
                .build();
        try (InputStream in = file.getInputStream()) {
            s3.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        }

        return new StoredFile(stored, file.getOriginalFilename(), publicBaseUrl + "/" + key,
                file.getContentType(), file.getSize());
    }

    @Override
    public void delete(String url) {
        if (url == null) return;
        String prefix = publicBaseUrl + "/";
        if (url.startsWith(prefix)) {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(url.substring(prefix.length()))
                    .build());
            return;
        }
        StoredFiles.deleteLocalFile(uploadDir, url);
    }
}
