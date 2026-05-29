package com.example.MyPickCafe.service;

import com.example.MyPickCafe.dto.PythonTagRequest;
import com.example.MyPickCafe.dto.PythonTagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonTagClient {

    private final RestTemplate restTemplate;

    @Value("${python.api.base-url}")
    private String baseUrl;

    /**
     * FastAPI POST /review/analyze 를 호출해 태그·감성 분석 결과를 반환한다.
     * 호출 실패(서버 다운, 타임아웃 등) 시 Optional.empty() 를 반환해 호출부에서 무시한다.
     */
    public Optional<PythonTagResponse> analyze(PythonTagRequest req) {
        try {
            ResponseEntity<PythonTagResponse> res = restTemplate.postForEntity(
                    baseUrl + "/review/analyze",
                    req,
                    PythonTagResponse.class
            );
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                return Optional.of(res.getBody());
            }
        } catch (Exception e) {
            log.warn("Python tag API 호출 실패 (무시됨): {}", e.getMessage());
        }
        return Optional.empty();
    }
}
