package com.memory_atelier.google.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        Integer expiresIn,

        @JsonProperty("refresh_token_expires_in")
        Integer refreshTokenExpiresIn
) {
}
