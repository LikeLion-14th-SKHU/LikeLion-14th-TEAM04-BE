package com.memory_atelier.kakao.application;

import com.memory_atelier.auth.JwtUtil;
import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.auth.domain.RefreshToken;
import com.memory_atelier.auth.domain.RefreshTokenRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.kakao.api.dto.response.KakaoTokenResponse;
import com.memory_atelier.kakao.api.dto.response.KakaoUserInfoResponse;
import com.memory_atelier.user.application.CreditPolicy;
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
public class KakaoOAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CreditPolicy creditPolicy;

    private final RestClient restClient = RestClient.create();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    @Transactional
    public LoginResponseDto kakaoLogin(String code) {

        KakaoTokenResponse tokenResponse = requestToken(code);

        KakaoUserInfoResponse userInfoResponse =
                requestUserInfo(tokenResponse.accessToken());

        String email = userInfoResponse.getEmail();
        String nickname = userInfoResponse.getNickname();

        User user = userRepository.findByEmailAndProvider(email, Provider.KAKAO)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .nickname(nickname)
                            .email(email)
                            .provider(Provider.KAKAO)
                            .password(null)
                            .build();
                    newUser.grantCredit(creditPolicy.signupGrant());
                    return userRepository.save(newUser);
                });

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

    private KakaoTokenResponse requestToken(String code) {
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
                    .body(KakaoTokenResponse.class);

        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.KAKAO_LOGIN_FAILED,
                    ErrorCode.KAKAO_LOGIN_FAILED.getMessage()
            );
        }
    }

    private KakaoUserInfoResponse requestUserInfo(String accessToken) {
        try {
            return restClient.get()
                    .uri(userInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.KAKAO_LOGIN_FAILED,
                    ErrorCode.KAKAO_LOGIN_FAILED.getMessage()
            );
        }
    }
}

