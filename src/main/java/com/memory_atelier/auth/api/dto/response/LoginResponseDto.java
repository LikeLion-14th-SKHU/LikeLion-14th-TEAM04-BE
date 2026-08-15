package com.memory_atelier.auth.api.dto.response;

public record LoginResponseDto(
        String accessToken,
        String refreshToken
) {
}
