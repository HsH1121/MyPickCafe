package com.example.MyPickCafe.api;

import com.example.MyPickCafe.TestPasswords;
import com.example.MyPickCafe.domain.CafeStatus;
import com.example.MyPickCafe.domain.RoleKind;
import com.example.MyPickCafe.entity.Cafe;
import com.example.MyPickCafe.entity.Member;
import com.example.MyPickCafe.repository.CafeRepository;
import com.example.MyPickCafe.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사업자 증빙 문서(사업자등록증) 비공개 저장 회귀 테스트.
 *
 * <p>수정 전에는 증빙 파일을 공개 업로드 폴더({@code /uploads/cafes/{id}/docs/})에 저장했고,
 * {@code /uploads/**} 는 보안 필터를 거치지 않아 URL 만 알면 로그인 없이 받을 수 있었다.
 * 관리자 전용 {@code /admin/cafes/{id}/bizdoc} 도 그 공개 URL로 302 리다이렉트할 뿐이었다.
 * 또 카페 등록 폼이 {@code bizDoc} 값을 그대로 받아서, 파일 없이 외부 URL이나 서버 경로를 넣으면
 * 관리자가 증빙을 열 때 외부 사이트로 이동하거나 서버의 임의 파일이 내려갔다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("사업자 증빙 비공개 저장")
class BizDocPrivacyTest {

    private static final String OWNER = "bizdoc-owner@example.com";
    private static final byte[] DOC = "%PDF-1.4 사업자등록증 원본".getBytes(StandardCharsets.UTF_8);

    @TempDir
    static Path tempRoot;

    @DynamicPropertySource
    static void storageDirs(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", () -> tempRoot.resolve("uploads").toString());
        registry.add("file.private-dir", () -> tempRoot.resolve("private").toString());
    }

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CafeRepository cafeRepository;

    private Member owner;
    private int phoneSeq;

    @BeforeEach
    void setUp() {
        owner = new Member();
        owner.setEmail(OWNER);
        owner.setPassword(TestPasswords.HASH);
        owner.setNickname("bizdoc-owner");
        owner.setRoleKind(RoleKind.MEMBER);
        owner.setTokenVersion(0L);
        owner = memberRepository.save(owner);
    }

    private static RequestPostProcessor ownerUser() { return user(OWNER).roles("MEMBER"); }
    private static RequestPostProcessor admin() { return user("bizdoc-admin@example.com").roles("ADMIN"); }

    private Path uploads() { return tempRoot.resolve("uploads"); }

    /** 화면의 카페 등록 폼과 같은 multipart 요청을 만든다. */
    private MockMultipartHttpServletRequestBuilder createForm(String name) {
        MockMultipartHttpServletRequestBuilder req = multipart("/cafes/create");
        req.param("name", name)
                .param("address", "서울시 마포구 " + name + "로 1")
                .param("number", "02-555-" + (1000 + phoneSeq++))
                .param("code", "CAFE")
                .with(ownerUser());
        return req;
    }

    private Cafe findByName(String name) {
        em.flush();
        em.clear();
        return em.createQuery("select c from Cafe c where c.name = :n", Cafe.class)
                .setParameter("n", name)
                .getSingleResult();
    }

    private Cafe legacyCafe(String name, String bizDoc) {
        Cafe c = new Cafe();
        c.setOwner(owner);
        c.setName(name);
        c.setAddress("서울시 마포구 " + name + "로 1");
        c.setNumber("02-666-" + (1000 + phoneSeq++));
        c.setCode("CAFE");
        c.setDate(LocalDateTime.now());
        c.setViews(0L);
        c.setStatus(CafeStatus.PENDING);
        c.setBizDoc(bizDoc);
        return cafeRepository.saveAndFlush(c);
    }

    private Path writeUpload(String relative, byte[] bytes) throws IOException {
        Path file = uploads().resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        return file;
    }

    // 이 카페의 공개 업로드 폴더(uploads/cafes/{id}) 안에서 증빙과 내용이 같은 파일을 찾는다
    private List<Path> publicFilesWithDocContent(Long cafeId) throws IOException {
        Path cafeDir = uploads().resolve("cafes/" + cafeId);
        if (!Files.exists(cafeDir)) return List.of();
        try (Stream<Path> files = Files.walk(cafeDir)) {
            return files.filter(Files::isRegularFile).filter(p -> {
                try {
                    return Arrays.equals(Files.readAllBytes(p), DOC);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        }
    }

    @Test
    @DisplayName("등록한 증빙 파일은 공개 업로드 폴더에 저장되지 않는다")
    void uploadedDocIsNotStoredInPublicFolder() throws Exception {
        mvc.perform(createForm("증빙비공개카페")
                        .file(new MockMultipartFile("bizDocFile", "사업자등록증.pdf", "application/pdf", DOC)))
                .andExpect(status().is3xxRedirection());

        Cafe cafe = findByName("증빙비공개카페");
        String stored = cafe.getBizDoc();
        assertThat(stored).isNotBlank();
        assertThat(stored).doesNotStartWith("/uploads").doesNotStartWith("/files").doesNotContain("://");
        assertThat(publicFilesWithDocContent(cafe.getId())).isEmpty();
    }

    @Test
    @DisplayName("관리자는 증빙 파일을 앱에서 직접 받는다(리다이렉트 없음, 캐시 금지)")
    void adminDownloadsDocDirectly() throws Exception {
        mvc.perform(createForm("증빙다운로드카페")
                        .file(new MockMultipartFile("bizDocFile", "사업자등록증.pdf", "application/pdf", DOC)))
                .andExpect(status().is3xxRedirection());
        Long id = findByName("증빙다운로드카페").getId();

        mvc.perform(get("/admin/cafes/{id}/bizdoc", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(DOC))
                // server.servlet.encoding.force=true 라 모든 응답에 charset 이 붙으므로 형식만 비교한다
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));

        // /admin/** 는 화면 경로라 권한이 없으면 SecurityConfig 가 403 대신 리다이렉트한다(파일은 나가지 않음)
        mvc.perform(get("/admin/cafes/{id}/bizdoc", id).with(ownerUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/admin/cafes/{id}/bizdoc", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("이미지가 아닌 파일(SVG·HTML 등)은 화면에 띄우지 않고 내려받기로만 준다")
    void nonWhitelistedTypesAreServedAsAttachment() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);
        mvc.perform(createForm("증빙SVG카페")
                        .file(new MockMultipartFile("bizDocFile", "doc.svg", "image/svg+xml", svg)))
                .andExpect(status().is3xxRedirection());
        Long id = findByName("증빙SVG카페").getId();

        mvc.perform(get("/admin/cafes/{id}/bizdoc", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    @DisplayName("등록 폼에 bizDoc 값을 직접 넣어도 저장되지 않는다(외부 이동·서버 파일 노출 차단)")
    void bizDocParamIsNotBound() throws Exception {
        Path serverSecret = Files.writeString(tempRoot.resolve("server-secret.txt"), "DB_PASSWORD=secret");

        mvc.perform(createForm("증빙외부URL카페").param("bizDoc", "https://evil.example/phish.pdf"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(createForm("증빙서버경로카페").param("bizDoc", serverSecret.toAbsolutePath().toString()))
                .andExpect(status().is3xxRedirection());

        Cafe external = findByName("증빙외부URL카페");
        Cafe localPath = findByName("증빙서버경로카페");
        assertThat(external.getBizDoc()).isNull();
        assertThat(localPath.getBizDoc()).isNull();

        mvc.perform(get("/admin/cafes/{id}/bizdoc", external.getId()).with(admin()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/admin/cafes/{id}/bizdoc", localPath.getId()).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("예전에 공개 폴더에 올라간 증빙도 공개 경로로는 받을 수 없다(사진은 그대로 공개)")
    void legacyPublicDocIsNotServed() throws Exception {
        Cafe cafe = legacyCafe("예전증빙카페", null);
        String docPath = "cafes/" + cafe.getId() + "/docs/legacy.pdf";
        writeUpload(docPath, DOC);
        writeUpload("cafes/" + cafe.getId() + "/photo.jpg", new byte[]{1, 2, 3});

        mvc.perform(get("/uploads/" + docPath)).andExpect(status().isNotFound());
        mvc.perform(get("/files/" + docPath)).andExpect(status().isNotFound());
        // Windows(NTFS)는 대소문자를 구분하지 않아 DOCS 로도 같은 파일이 열린다
        mvc.perform(get("/uploads/cafes/" + cafe.getId() + "/DOCS/legacy.pdf")).andExpect(status().isNotFound());

        mvc.perform(get("/uploads/cafes/" + cafe.getId() + "/photo.jpg")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("이전 작업은 자기 카페 docs 폴더의 예전 증빙만 비공개 저장소로 옮긴다")
    void migrateMovesOnlyOwnLegacyDocs() throws Exception {
        Cafe legacy = legacyCafe("이전대상카페", null);
        String legacyRel = "cafes/" + legacy.getId() + "/docs/legacy.pdf";
        Path legacyFile = writeUpload(legacyRel, DOC);
        legacy.setBizDoc("/uploads/" + legacyRel);
        cafeRepository.saveAndFlush(legacy);

        // 수정 전 폼 바인딩으로 들어갔을 수 있는 값: 다른 카페의 공개 사진, 외부 URL
        Cafe victim = legacyCafe("사진주인카페", null);
        Path victimPhoto = writeUpload("cafes/" + victim.getId() + "/photo.jpg", new byte[]{9, 9, 9});
        Cafe pointsToVictim = legacyCafe("남의사진가리킴카페", "/uploads/cafes/" + victim.getId() + "/photo.jpg");
        Cafe external = legacyCafe("외부URL카페", "https://evil.example/phish.pdf");
        Cafe missing = legacyCafe("파일없음카페", null);
        missing.setBizDoc("/uploads/cafes/" + missing.getId() + "/docs/gone.pdf");
        cafeRepository.saveAndFlush(missing);

        mvc.perform(post("/admin/cafes/bizdocs/migrate").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.migrated").value(1))
                .andExpect(jsonPath("$.skipped").value(2))
                .andExpect(jsonPath("$.missing").value(1));

        Cafe migrated = findByName("이전대상카페");
        assertThat(migrated.getBizDoc()).doesNotStartWith("/uploads");
        assertThat(legacyFile).doesNotExist();
        mvc.perform(get("/admin/cafes/{id}/bizdoc", legacy.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(DOC));

        assertThat(victimPhoto).exists();
        assertThat(findByName("남의사진가리킴카페").getBizDoc()).isEqualTo(pointsToVictim.getBizDoc());
        assertThat(findByName("외부URL카페").getBizDoc()).isEqualTo(external.getBizDoc());

        // 두 번 돌려도 이미 옮긴 것은 건드리지 않는다
        mvc.perform(post("/admin/cafes/bizdocs/migrate").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.migrated").value(0));
    }
}
