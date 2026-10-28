package com.example.MyPickCafe.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3FileStorageServiceTest {

    @TempDir Path uploadDir;

    private final S3Client s3 = mock(S3Client.class);

    private S3FileStorageService service(String publicBaseUrl) {
        return new S3FileStorageService(s3, "mpc-bucket", "ap-northeast-2", publicBaseUrl, uploadDir.toString());
    }

    @Test
    void store_putsObjectUnderSubDirAndReturnsPublicUrl() throws Exception {
        var file = new MockMultipartFile("file", "라떼.jpg", "image/jpeg", new byte[]{1, 2, 3});

        var stored = service("").store(file, "/cafes/7/");

        var req = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(req.capture(), any(RequestBody.class));
        assertThat(req.getValue().bucket()).isEqualTo("mpc-bucket");
        assertThat(req.getValue().key()).matches("cafes/7/[0-9a-f]{32}\\.jpg");
        assertThat(req.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(stored.url())
                .isEqualTo("https://mpc-bucket.s3.ap-northeast-2.amazonaws.com/" + req.getValue().key());
        assertThat(stored.originalName()).isEqualTo("라떼.jpg");
        assertThat(stored.sizeBytes()).isEqualTo(3);
    }

    @Test
    void store_usesConfiguredPublicBaseUrl() throws Exception {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        String url = service("https://cdn.example.com/").save(file, "menus/1");

        assertThat(url).matches("https://cdn\\.example\\.com/menus/1/[0-9a-f]{32}\\.png");
    }

    @Test
    void delete_s3UrlDeletesObjectByKey() {
        service("").delete("https://mpc-bucket.s3.ap-northeast-2.amazonaws.com/cafes/7/abc.jpg");

        var req = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(req.capture());
        assertThat(req.getValue().bucket()).isEqualTo("mpc-bucket");
        assertThat(req.getValue().key()).isEqualTo("cafes/7/abc.jpg");
    }

    @Test
    void delete_legacyLocalUrlDeletesLocalFileWithoutS3Call() throws Exception {
        Path legacy = uploadDir.resolve("cafes/7/old.jpg");
        Files.createDirectories(legacy.getParent());
        Files.write(legacy, new byte[]{1});

        service("").delete("/uploads/cafes/7/old.jpg");

        assertThat(legacy).doesNotExist();
        verifyNoInteractions(s3);
    }

    @Test
    void delete_ignoresForeignUrlAndPathOutsideUploadDir() throws Exception {
        Path outside = uploadDir.getParent().resolve("outside-" + System.nanoTime() + ".txt");
        Files.write(outside, new byte[]{1});
        try {
            service("").delete("https://other-bucket.s3.amazonaws.com/cafes/7/abc.jpg");
            service("").delete("/uploads/../" + outside.getFileName());

            assertThat(outside).exists();
            verifyNoInteractions(s3);
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void storedValue_keepsOnlyKeyForOwnUrlsAndLeavesOthers() {
        var s = service("https://cdn.example.com");

        assertThat(s.toStoredValue("https://cdn.example.com/cafes/7/a.jpg")).isEqualTo("cafes/7/a.jpg");
        assertThat(s.toStoredValue("/uploads/cafes/7/old.jpg")).isEqualTo("/uploads/cafes/7/old.jpg");
        assertThat(s.toStoredValue("https://other.example.com/x.jpg")).isEqualTo("https://other.example.com/x.jpg");
        assertThat(s.toStoredValue(null)).isNull();
    }

    @Test
    void toUrl_prefixesKeysOnly() {
        var s = service("https://cdn.example.com");

        assertThat(s.toUrl("cafes/7/a.jpg")).isEqualTo("https://cdn.example.com/cafes/7/a.jpg");
        assertThat(s.toUrl("/uploads/cafes/7/old.jpg")).isEqualTo("/uploads/cafes/7/old.jpg");
        assertThat(s.toUrl("https://other.example.com/x.jpg")).isEqualTo("https://other.example.com/x.jpg");
        assertThat(s.toUrl("")).isEmpty();
        assertThat(s.toUrl(null)).isNull();
    }

    @Test
    void constructor_failsFastWithoutBucket() {
        assertThatThrownBy(() -> new S3FileStorageService(s3, "", "ap-northeast-2", "", uploadDir.toString()))
                .isInstanceOf(IllegalStateException.class);
    }
}
