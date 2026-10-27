package com.example.MyPickCafe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// file.storage=s3 일 때만 S3 클라이언트를 만든다.
// 자격 증명은 SDK 기본 체인: EC2 인스턴스 역할(IAM Role) → 환경 변수 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY 순으로 찾는다.
@Configuration
@ConditionalOnProperty(name = "file.storage", havingValue = "s3")
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(@Value("${file.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
