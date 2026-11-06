package com.example.MyPickCafe.service;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.CafePhoto;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.entity.Menu;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// file.storage=s3 일 때 DB 컬럼에는 객체 키만 들어가고, 엔티티·조회 결과는 공개 URL을 돌려주는지 확인한다
@SpringBootTest(properties = {
        "file.storage=s3",
        "file.s3.bucket=mpc-bucket",
        "file.s3.region=ap-northeast-2"
})
@ActiveProfiles("test")
@Transactional
class StoredFileKeyPersistenceTest {

    private static final String BASE = "https://mpc-bucket.s3.ap-northeast-2.amazonaws.com/";
    private static final String BIZ_DOC_KEY = "bizdocs/1/0123456789abcdef0123456789abcdef.pdf";

    @MockitoBean S3Client s3;
    @Autowired EntityManager em;
    @Autowired CafePhotoService cafePhotoService;

    private Cafe cafe;

    @BeforeEach
    void setUp() {
        Member owner = new Member();
        owner.setEmail("s3-key-test@example.com");
        owner.setPassword(TestPasswords.HASH);
        owner.setNickname("s3-key-owner");
        owner.setRoleKind(RoleKind.CAFEOWNER);
        em.persist(owner);

        cafe = new Cafe();
        cafe.setOwner(owner);
        cafe.setName("S3 key test cafe");
        cafe.setAddress("Test address");
        cafe.setNumber("02-111-1111");
        cafe.setCode("CAFE");
        cafe.setDate(LocalDateTime.now());
        cafe.setViews(0L);
        cafe.setStatus(CafeStatus.APPROVED);
        cafe.setBizDoc(BIZ_DOC_KEY);
        em.persist(cafe);
        em.flush();
    }

    @Test
    void s3UrlsAreStoredAsKeysAndReadBackAsUrls() {
        CafePhoto photo = new CafePhoto();
        photo.setCafe(cafe);
        photo.setUrl(BASE + "cafes/1/a.jpg");
        em.persist(photo);

        Menu menu = new Menu();
        menu.setCafe(cafe);
        menu.setName("라떼");
        menu.setPrice(5000);
        menu.setPhoto(BASE + "menus/1/m.png");
        em.persist(menu);
        em.flush();

        assertThat(column("select url from cafe_photo where cafe_photo_id = " + photo.getId()))
                .isEqualTo("cafes/1/a.jpg");
        assertThat(column("select photo from menu where menu_id = " + menu.getId()))
                .isEqualTo("menus/1/m.png");
        // 사업자 증빙은 공개 URL 이 아니라 비공개 저장소 키라서 변환 없이 그대로 저장·조회된다
        assertThat(column("select biz_doc from cafe where cafe_id = " + cafe.getId()))
                .isEqualTo(BIZ_DOC_KEY);

        em.clear();
        assertThat(em.find(CafePhoto.class, photo.getId()).getUrl()).isEqualTo(BASE + "cafes/1/a.jpg");
        assertThat(em.find(Menu.class, menu.getId()).getPhoto()).isEqualTo(BASE + "menus/1/m.png");
        assertThat(em.find(Cafe.class, cafe.getId()).getBizDoc()).isEqualTo(BIZ_DOC_KEY);
    }

    @Test
    void legacyLocalPathsAreLeftAsIs() {
        em.createNativeQuery("update cafe set biz_doc = '/uploads/cafes/1/docs/old.pdf' where cafe_id = " + cafe.getId())
                .executeUpdate();
        insertPhoto("/uploads/cafes/1/old.jpg");
        em.clear();

        assertThat(em.find(Cafe.class, cafe.getId()).getBizDoc()).isEqualTo("/uploads/cafes/1/docs/old.pdf");
        assertThat(em.createQuery("select p.url from CafePhoto p", String.class).getResultList())
                .containsExactly("/uploads/cafes/1/old.jpg");
    }

    @Test
    void mainPhotoUrlsResolveKeysFromNativeQuery() {
        insertPhoto("cafes/1/main.jpg");

        assertThat(cafePhotoService.findMainPhotoUrls(List.of(cafe.getId())))
                .containsEntry(cafe.getId(), BASE + "cafes/1/main.jpg");
    }

    private void insertPhoto(String storedValue) {
        em.createNativeQuery("insert into cafe_photo (cafe_id, url, sort_index, is_main) values (?, ?, 0, true)")
                .setParameter(1, cafe.getId())
                .setParameter(2, storedValue)
                .executeUpdate();
    }

    private Object column(String sql) {
        return em.createNativeQuery(sql).getSingleResult();
    }
}
