package com.memory_atelier.auth.application;


import com.memory_atelier.auth.JwtUtil;
import com.memory_atelier.auth.api.dto.request.LoginRequestDto;
import com.memory_atelier.auth.api.dto.request.SignupRequestDto;
import com.memory_atelier.auth.api.dto.request.TokenRefreshRequestDto;
import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.auth.api.dto.response.TokenRefreshResponseDto;
import com.memory_atelier.auth.domain.RefreshToken;
import com.memory_atelier.auth.domain.RefreshTokenRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.user.application.CreditPolicy;
import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CreditPolicy creditPolicy;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // 회원가입
    @Transactional
    public void signup(SignupRequestDto request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL, ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        User user = User.builder()
                .nickname(request.nickname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(Provider.LOCAL)
                .build();
        user.grantCredit(creditPolicy.signupGrant());

        userRepository.save(user);
    }

    // 로그인
    // 리프레시 토큰 삭제·저장(쓰기)이 있으므로 클래스 기본값(readOnly=true)을 오버라이드해야 한다
    // H2는 읽기 전용 커넥션에서도 쓰기를 조용히 허용해서 지금까지 안 드러났지만
    // MySQL은 커넥션 자체를 read-only로 열어서 쓰기 시도 시 SQLException을 던진다
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {

        User user = userRepository.findByEmailAndProvider(request.email(), Provider.LOCAL)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        String accessToken = jwtUtil.generateToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        refreshTokenRepository.deleteByUserId(user.getUserId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                        .userId(user.getUserId())
                        .token(refreshToken)
                        .expiredAt(LocalDateTime.now().plusSeconds(refreshExpiration/1000))
                        .build();

        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponseDto(accessToken, refreshToken);
    }

    // 토큰 재발급
    @Transactional
    public TokenRefreshResponseDto refresh(TokenRefreshRequestDto request) {

        String refreshToken = request.refreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND, ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage()));

        if (savedToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN, ErrorCode.EXPIRED_REFRESH_TOKEN.getMessage());
        }

        User user = userRepository.findById(savedToken.getUserId())
                .orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));

        String newAccessToken = jwtUtil.generateToken(user.getUserId(), user.getRole());

        return new TokenRefreshResponseDto(newAccessToken);
    }

}
