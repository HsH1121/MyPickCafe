package com.example.MyPickCafe.config;

import com.example.MyPickCafe.service.FileStorageService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 업로드 파일 컬럼(cafe_photo.url, menu.photo, cafe.biz_doc)의 DB 값 ↔ 엔티티 값 변환.
 * 엔티티·템플릿·API는 늘 화면에서 쓸 URL을 다루고, DB에는 저장소가 정한 값(S3면 객체 키)만 들어간다.
 * Hibernate 가 Spring 빈 컨테이너로 만들어서 현재 저장소 구현을 생성자로 받는다.
 * 네이티브 쿼리로 컬럼을 직접 읽을 때는 이 변환을 거치지 않으니 FileStorageService.toUrl 을 직접 호출해야 한다.
 */
@Converter
public class StoredFileUrlConverter implements AttributeConverter<String, String> {

    private final FileStorageService storage;

    public StoredFileUrlConverter(FileStorageService storage) {
        this.storage = storage;
    }

    @Override
    public String convertToDatabaseColumn(String url) {
        return storage.toStoredValue(url);
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        return storage.toUrl(storedValue);
    }
}
