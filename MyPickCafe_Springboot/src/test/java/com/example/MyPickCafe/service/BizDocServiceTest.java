package com.example.MyPickCafe.service;

import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.repository.CafeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// 로컬 이전·다운로드는 BizDocPrivacyTest 가 확인한다. 여기서는 S3 시절 키와 잘못된 값의 처리만 본다.
class BizDocServiceTest {

    private static final String S3_URL = "https://mpc-bucket.s3.ap-northeast-2.amazonaws.com/";
    private static final String HEX = "0123456789abcdef0123456789abcdef";

    private final PrivateFileStorage privateStorage = mock(PrivateFileStorage.class);
    private final FileStorageService publicStorage = mock(FileStorageService.class);
    private final CafeRepository cafeRepository = mock(CafeRepository.class);
    private final BizDocService bizDocService = new BizDocService(privateStorage, publicStorage, cafeRepository);

    private static Cafe cafe(long cafeId, String bizDoc) {
        Cafe c = new Cafe();
        c.setId(cafeId);
        c.setBizDoc(bizDoc);
        return c;
    }

    @Test
    void migrate_s3LegacyKeyMovesToPrivateStorageAndDeletesPublicObject() throws Exception {
        Cafe cafe = cafe(7, "cafes/7/docs/abc.PDF");
        when(cafeRepository.findByBizDocIsNotNull()).thenReturn(List.of(cafe));
        when(publicStorage.toUrl("cafes/7/docs/abc.PDF")).thenReturn(S3_URL + "cafes/7/docs/abc.PDF");
        when(publicStorage.read(S3_URL + "cafes/7/docs/abc.PDF")).thenReturn(Optional.of(new byte[]{1, 2}));

        var result = bizDocService.migrateLegacy();

        assertThat(result).isEqualTo(new BizDocService.MigrationResult(1, 0, 0));
        assertThat(cafe.getBizDoc()).matches("bizdocs/7/[0-9a-f]{32}\\.pdf");
        verify(privateStorage).put(eq(cafe.getBizDoc()), aryEq(new byte[]{1, 2}), eq(null));
        verify(publicStorage).delete(S3_URL + "cafes/7/docs/abc.PDF");
    }

    @Test
    void migrate_skipsValuesOutsideOwnDocsFolder() throws Exception {
        when(cafeRepository.findByBizDocIsNotNull()).thenReturn(List.of(
                cafe(7, "cafes/8/docs/abc.pdf"),              // 다른 카페
                cafe(7, "cafes/7/docs/../photo.jpg"),         // 상위 폴더
                cafe(7, "cafes/7/docs/sub/x.pdf"),            // 하위 폴더
                cafe(7, "/uploads/cafes/7/docs/../../8/a.jpg"),
                cafe(7, "C:\\secret.txt"),                   // 서버 경로
                cafe(7, "bizdocs/8/" + HEX + ".pdf")          // 다른 카페의 비공개 키
        ));

        var result = bizDocService.migrateLegacy();

        assertThat(result).isEqualTo(new BizDocService.MigrationResult(0, 6, 0));
        verifyNoInteractions(privateStorage);
        verify(publicStorage, never()).read(anyString());
        verify(publicStorage, never()).delete(anyString());
    }

    @Test
    void migrate_leavesOwnPrivateKeysAlone() throws Exception {
        Cafe cafe = cafe(7, "bizdocs/7/" + HEX + ".pdf");
        when(cafeRepository.findByBizDocIsNotNull()).thenReturn(List.of(cafe));

        assertThat(bizDocService.migrateLegacy()).isEqualTo(new BizDocService.MigrationResult(0, 0, 0));
        assertThat(cafe.getBizDoc()).isEqualTo("bizdocs/7/" + HEX + ".pdf");
        verifyNoInteractions(privateStorage);
    }

    @Test
    void load_readsOnlyThisCafesPrivateKey() throws Exception {
        assertThat(bizDocService.load(cafe(7, "bizdocs/8/" + HEX + ".pdf"))).isEmpty();
        assertThat(bizDocService.load(cafe(7, "/uploads/cafes/7/docs/a.pdf"))).isEmpty();
        assertThat(bizDocService.load(cafe(7, null))).isEmpty();
        verifyNoInteractions(privateStorage);

        when(privateStorage.get("bizdocs/7/" + HEX + ".png")).thenReturn(Optional.of(new byte[]{3}));
        var doc = bizDocService.load(cafe(7, "bizdocs/7/" + HEX + ".png")).orElseThrow();
        assertThat(doc.content()).containsExactly(3);
        assertThat(doc.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(doc.inline()).isTrue();
        assertThat(doc.fileName()).isEqualTo("bizdoc-7.png");
    }
}
