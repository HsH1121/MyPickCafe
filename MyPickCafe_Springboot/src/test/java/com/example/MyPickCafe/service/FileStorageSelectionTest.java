package com.example.MyPickCafe.service;

import com.example.MyPickCafe.config.S3Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

// file.storage 값에 따라 저장소 구현이 하나만 뜨는지 확인한다(S3 연결은 하지 않는다)
class FileStorageSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(S3Config.class, LocalFileStorageService.class, S3FileStorageService.class)
            .withPropertyValues("file.upload-dir=./build/test-uploads", "file.s3.region=ap-northeast-2");

    @Test
    void defaultsToLocalStorage() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(FileStorageService.class);
            assertThat(ctx.getBean(FileStorageService.class)).isInstanceOf(LocalFileStorageService.class);
            assertThat(ctx).doesNotHaveBean(S3Client.class);
        });
    }

    @Test
    void s3StorageWhenConfigured() {
        runner.withPropertyValues("file.storage=s3", "file.s3.bucket=mpc-bucket").run(ctx -> {
            assertThat(ctx).hasSingleBean(FileStorageService.class);
            assertThat(ctx.getBean(FileStorageService.class)).isInstanceOf(S3FileStorageService.class);
        });
    }

    @Test
    void s3StorageWithoutBucketFailsToStart() {
        runner.withPropertyValues("file.storage=s3").run(ctx -> assertThat(ctx).hasFailed());
    }
}
