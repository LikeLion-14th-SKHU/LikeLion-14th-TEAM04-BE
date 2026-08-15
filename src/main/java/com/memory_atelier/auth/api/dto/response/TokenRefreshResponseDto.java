package com.memory_atelier.auth.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "액세스 토큰 재발급 응답")
public record TokenRefreshResponseDto(
        @Schema(description = "새로 발급된 액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken
) {
}
