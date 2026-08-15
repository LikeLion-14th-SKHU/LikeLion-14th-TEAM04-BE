package com.memory_atelier.auth.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "액세스 토큰 재발급 요청")
public record TokenRefreshRequestDto(
        @Schema(description = "로그인 시 발급받은 리프레시 토큰", example = "eyJhbGciOiJIUzUxMiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {
}
