package com.memory_atelier.google.application;

import com.memory_atelier.auth.JwtUtil;
import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.auth.domain.RefreshToken;
import com.memory_atelier.auth.domain.RefreshTokenRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.google.api.dto.response.GoogleTokenResponse;
import com.memory_atelier.google.api.dto.response.GoogleUserInfoResponse;
import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    private final RestClient restClient = RestClient.create();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @Value("${google.token-uri}")
    private String tokenUri;

    @Value("${google.user-info-uri}")
    private String userInfoUri;

    @Transactional
    public LoginResponseDto googleLogin(String code) {

        GoogleTokenResponse tokenResponse = requestToken(code);

        GoogleUserInfoResponse userInfoResponse =
                requestUserInfo(tokenResponse.accessToken());

        String email = userInfoResponse.getEmail();
        String nickname = userInfoResponse.getNickname();

        User user = userRepository.findByEmailAndProvider(email, Provider.GOOGLE)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .nickname(nickname)
                                .email(email)
                                .provider(Provider.GOOGLE)
                                .password(null)
                                .build()
                ));

        String accessToken = jwtUtil.generateToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        refreshTokenRepository.deleteByUserId(user.getUserId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshToken)
                .expiredAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return new LoginResponseDto(accessToken, refreshToken);
    }

    private GoogleTokenResponse requestToken(String code) {
        try {
            return restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(
                            "grant_type=authorization_code"
                                    + "&client_id=" + clientId
                                    + "&client_secret=" + clientSecret
                                    + "&redirect_uri=" + redirectUri
                                    + "&code=" + code
                    )
                    .retrieve()
                    .body(GoogleTokenResponse.class);

        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.GOOGLE_LOGIN_FAILED,
                    ErrorCode.GOOGLE_LOGIN_FAILED.getMessage()
            );
        }
    }

    private GoogleUserInfoResponse requestUserInfo(String accessToken) {
        try {
            return restClient.get()
                    .uri(userInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.GOOGLE_LOGIN_FAILED,
                    ErrorCode.GOOGLE_LOGIN_FAILED.getMessage()
            );
        }
    }
}