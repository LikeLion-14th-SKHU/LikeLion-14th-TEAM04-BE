package com.memory_atelier.naver.application;

import com.memory_atelier.auth.JwtUtil;
import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.auth.domain.RefreshToken;
import com.memory_atelier.auth.domain.RefreshTokenRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.naver.api.dto.response.NaverTokenResponse;
import com.memory_atelier.naver.api.dto.response.NaverUserInfoResponse;
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
public class NaverOAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CreditPolicy creditPolicy;

    private final RestClient restClient = RestClient.create();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @Value("${naver.redirect-uri}")
    private String redirectUri;

    @Value("${naver.token-uri}")
    private String tokenUri;

    @Value("${naver.user-info-uri}")
    private String userInfoUri;

    @Transactional
    public LoginResponseDto naverLogin(String code, String state) {

        NaverTokenResponse tokenResponse = requestToken(code, state);

        NaverUserInfoResponse userInfoResponse =
                requestUserInfo(tokenResponse.accessToken());

        String email = userInfoResponse.getEmail();
        String nickname = userInfoResponse.getNickname();

        User user = userRepository.findByEmailAndProvider(email, Provider.NAVER)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .nickname(nickname)
                            .email(email)
                            .provider(Provider.NAVER)
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

    private NaverTokenResponse requestToken(String code, String state) {
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
                                    + "&state=" + state
                    )
                    .retrieve()
                    .body(NaverTokenResponse.class);

        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.NAVER_LOGIN_FAILED,
                    ErrorCode.NAVER_LOGIN_FAILED.getMessage()
            );
        }
    }

    private NaverUserInfoResponse requestUserInfo(String accessToken) {
        try {
            return restClient.get()
                    .uri(userInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(NaverUserInfoResponse.class);

        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.NAVER_LOGIN_FAILED,
                    ErrorCode.NAVER_LOGIN_FAILED.getMessage()
            );
        }
    }
}


