package com.memory_atelier.global.oauth;

import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class OAuthRedirectHelper {

    @Value("${frontend.oauth-redirect-uri}")
    private String frontendUri;

    public ResponseEntity<Void> redirectWithTokens(LoginResponseDto loginResponse) {
        URI redirectUri = UriComponentsBuilder
                .fromUriString(frontendUri)
                .queryParam("accessToken", loginResponse.accessToken())
                .queryParam("refreshToken", loginResponse.refreshToken())
                .build()
                .toUri();

        return ResponseEntity.status(302).location(redirectUri).build();
    }
}
