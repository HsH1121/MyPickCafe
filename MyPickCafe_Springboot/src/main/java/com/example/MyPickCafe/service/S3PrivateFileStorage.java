package com.example.MyPickCafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

/**
 * 사진과 같은 버킷의 {@code private/} 아래에 저장한다.
 * 버킷의 공개 읽기 정책(또는 CloudFront)은 {@code cafes/*}·{@code menus/*} 처럼 사진 경로에만 걸고
 * {@code private/*} 는 빼야 한다. 앱은 EC2 인스턴스 역할로 읽는다(s3:GetObject 필요).
 */
@Service
@ConditionalOnProperty(name = "file.storage", havingValue = "s3")
public class S3PrivateFileStorage implements PrivateFileStorage {

    static final String PREFIX = "private/";

    private final S3Client s3;
    private final String bucket;

    public S3PrivateFileStorage(S3Client s3, @Value("${file.s3.bucket:}") String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("file.storage=s3 이면 file.s3.bucket(S3_BUCKET)을 지정해야 합니다.");
        }
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(PREFIX + key)
                        .contentType(contentType)
                        .cacheControl("private, no-store")
                        .build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public Optional<byte[]> get(String key) {
        try {
            return Optional.of(s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(PREFIX + key)
                    .build()).asByteArray());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(PREFIX + key)
                .build());
    }
}
