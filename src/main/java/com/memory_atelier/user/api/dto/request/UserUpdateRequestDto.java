package com.memory_atelier.user.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDto (
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 255, message =  "닉네임은 255자 이하여야 합니다.")
    String nickname,

    String profileImageUrl
) {
}
