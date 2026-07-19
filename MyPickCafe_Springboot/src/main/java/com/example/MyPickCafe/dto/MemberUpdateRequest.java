package com.example.MyPickCafe.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원 수정 요청 DTO.
 *
 * <p>이메일과 비밀번호는 이 엔드포인트에서 변경할 수 없다.
 * 비밀번호 변경은 현재 비밀번호 확인이 필요하므로
 * {@code MemberService#updateSelf} 경로로만 처리한다.
 * null 필드는 "변경하지 않음"을 의미한다.
 */
public record MemberUpdateRequest(
        @Size(max = 20)
        String nickname,

        @Min(0) @Max(150)
        Long age,

        @Pattern(regexp = "[MF]", message = "성별은 M 또는 F여야 합니다.")
        String gender,

        /** MEMBER / CAFEOWNER / ADMIN */
        String roleKind,

        @Size(max = 30)
        String photo
) {
}
