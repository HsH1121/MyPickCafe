package com.example.MyPickCafe.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 비공개 저장소: 로컬은 file.private-dir 밖으로 못 나가고, S3 는 private/ 아래에만 쓰는지 확인한다
class PrivateFileStorageTest {

    @TempDir Path dir;

    @Test
    void local_putGetDeleteRoundTrip() throws Exception {
        var storage = new LocalPrivateFileStorage(dir.toString());

        storage.put("bizdocs/7/a.pdf", new byte[]{1, 2, 3}, "application/pdf");

        assertThat(dir.resolve("bizdocs/7/a.pdf")).exists();
        assertThat(storage.get("bizdocs/7/a.pdf")).contains(new byte[]{1, 2, 3});
        storage.delete("bizdocs/7/a.pdf");
        assertThat(storage.get("bizdocs/7/a.pdf")).isEmpty();
    }

    @Test
    void local_rejectsKeysOutsideRoot() throws Exception {
        var storage = new LocalPrivateFileStorage(dir.resolve("private").toString());
        Path outside = Files.writeString(dir.resolve("secret.txt"), "secret");

        assertThatThrownBy(() -> storage.get("../secret.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.get(outside.toAbsolutePath().toString()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.put("", new byte[]{1}, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void s3_writesUnderPrivatePrefixWithoutPublicCache() {
        S3Client s3 = mock(S3Client.class);
        var storage = new S3PrivateFileStorage(s3, "mpc-bucket");

        storage.put("bizdocs/7/a.pdf", new byte[]{1}, "application/pdf");

        var req = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(req.capture(), any(RequestBody.class));
        assertThat(req.getValue().bucket()).isEqualTo("mpc-bucket");
        assertThat(req.getValue().key()).isEqualTo("private/bizdocs/7/a.pdf");
        assertThat(req.getValue().cacheControl()).doesNotContain("public").contains("no-store");
    }

    @Test
    void s3_getReadsPrivateKeyAndMissingIsEmpty() {
        S3Client s3 = mock(S3Client.class);
        var storage = new S3PrivateFileStorage(s3, "mpc-bucket");
        when(s3.getObjectAsBytes(GetObjectRequest.builder().bucket("mpc-bucket").key("private/bizdocs/7/a.pdf").build()))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[]{5}));
        when(s3.getObjectAsBytes(GetObjectRequest.builder().bucket("mpc-bucket").key("private/bizdocs/7/none.pdf").build()))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThat(storage.get("bizdocs/7/a.pdf")).contains(new byte[]{5});
        assertThat(storage.get("bizdocs/7/none.pdf")).isEmpty();
    }

    @Test
    void s3_deleteRemovesPrivateKey() {
        S3Client s3 = mock(S3Client.class);
        new S3PrivateFileStorage(s3, "mpc-bucket").delete("bizdocs/7/a.pdf");

        var req = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(req.capture());
        assertThat(req.getValue().key()).isEqualTo("private/bizdocs/7/a.pdf");
    }

    @Test
    void s3_requiresBucket() {
        assertThatThrownBy(() -> new S3PrivateFileStorage(mock(S3Client.class), " "))
                .isInstanceOf(IllegalStateException.class);
    }
}
