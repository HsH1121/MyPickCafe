package com.example.MyPickCafe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원 생성 요청 DTO.
 *
 * <p>엔티티를 그대로 @RequestBody로 받으면 클라이언트가 {@code id},
 * {@code tokenVersion}, {@code roleKind} 같은 필드를 임의로 지정할 수 있다.
 * 받을 값만 명시적으로 정의해 이를 차단한다.
 */
public record MemberCreateRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100)
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 20)
        String nickname,

        @Min(0) @Max(150)
        Long age,

        @Pattern(regexp = "[MF]", message = "성별은 M 또는 F여야 합니다.")
        String gender,

        /** MEMBER / CAFEOWNER / ADMIN. 미지정 시 MEMBER. */
        String roleKind,

        @Size(max = 30)
        String photo
) {
}
