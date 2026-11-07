// src/main/java/com/example/MyPickCafe/config/WebConfig.java
package com.example.MyPickCafe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}") // ← ./uploads 그대로 둬도 됨
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();

        // 예) file:/C:/proj/MyPickCafe/uploads/ — /files/** 는 과거 URL 호환
        registry.addResourceHandler("/uploads/**", "/files/**")
                .addResourceLocations(root.toUri().toString())
                .resourceChain(false)
                .addResolver(new PublicUploadResolver(root));
    }

    /**
     * 예전에 공개 폴더(cafes/{cafeId}/docs/)에 저장된 사업자 증빙은 /uploads 로 내려주지 않는다.
     * 새 증빙은 비공개 저장소(file.private-dir)에 두지만, 배포 후 이전 작업
     * (POST /admin/cafes/bizdocs/migrate) 전까지 남아 있는 파일을 막는다.
     * 요청 경로가 아니라 디스크의 실제 경로로 판단해서 대소문자(Windows)·심볼릭 링크 같은 변형도 막는다.
     */
    static class PublicUploadResolver extends PathResourceResolver {

        private static final Pattern BIZ_DOC_DIR =
                Pattern.compile("^cafes/[^/]+/docs(/.*)?$", Pattern.CASE_INSENSITIVE);

        private final Path root;

        PublicUploadResolver(Path root) {
            this.root = root;
        }

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            if (resource == null) return null;

            Path rootReal = root.toRealPath();
            Path real = resource.getFile().toPath().toRealPath();
            if (!real.startsWith(rootReal)) return null;
            String relative = rootReal.relativize(real).toString().replace('\\', '/');
            return BIZ_DOC_DIR.matcher(relative).matches() ? null : resource;
        }
    }
}
