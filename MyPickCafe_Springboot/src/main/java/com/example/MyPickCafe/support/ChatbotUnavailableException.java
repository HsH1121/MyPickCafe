package com.example.MyPickCafe.support;

import lombok.Getter;

/**
 * 추천 챗봇 서버 호출이 실패했음을 나타낸다.
 *
 * <p>"조건에 맞는 카페가 없음(빈 결과)"과 "추천 자체를 받지 못함"을 구분하기 위해
 * 실패를 빈 목록으로 흡수하지 않고 이 예외로 올린다. API 응답은 503 으로 변환된다.
 */
@Getter
public class ChatbotUnavailableException extends RuntimeException {

    public enum Reason {
        /** 연결 거부, 연결 타임아웃, 호스트 해석 실패 등 — 서버에 닿지 못함 */
        CONNECTION_FAILED,
        /** 연결은 됐지만 응답 제한 시간 안에 응답이 오지 않음 */
        TIMEOUT,
        /** 서버가 4xx/5xx 로 응답 */
        HTTP_ERROR,
        /** 응답 본문을 결과 DTO 로 변환하지 못함 */
        INVALID_RESPONSE,
        /** 위로 분류되지 않은 실패 */
        UNKNOWN
    }

    private final Reason reason;

    public ChatbotUnavailableException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }
}
