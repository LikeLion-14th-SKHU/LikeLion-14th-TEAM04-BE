package com.memory_atelier.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원 정보 수정 요청")
public record UserUpdateRequestDto (
    @Schema(description = "변경할 닉네임", example = "구름빵", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 255, message =  "닉네임은 255자 이하여야 합니다.")
    String nickname
) {
}
