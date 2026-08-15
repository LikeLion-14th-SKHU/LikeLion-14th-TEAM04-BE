package com.memory_atelier.auth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "로컬(이메일/비밀번호) 회원가입 요청")
public record SignupRequestDto(

        @Schema(description = "닉네임", example = "구름빵", minLength = 1, maxLength = 10, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 10, message = "닉네임은 1자 이상 10자 이하로 입력해주세요.")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "비밀번호. 영문·숫자·특수문자를 각각 1개 이상 포함, 8~20자", example = "memory1234!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+]).*$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 최소 1개 이상 포함해야 합니다."
        )
        String password
) {
}
