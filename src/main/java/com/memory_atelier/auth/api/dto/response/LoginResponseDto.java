package com.memory_atelier.auth.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 성공 응답 (로컬 로그인 및 소셜 로그인 공통)")
public record LoginResponseDto(
        @Schema(description = "액세스 토큰. Authorization 헤더에 `Bearer {accessToken}` 형식으로 담아 사용", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken,

        @Schema(description = "리프레시 토큰. 액세스 토큰 만료 시 `/auth/reissue`에 사용", example = "eyJhbGciOiJIUzUxMiJ9...")
        String refreshToken
) {
}
