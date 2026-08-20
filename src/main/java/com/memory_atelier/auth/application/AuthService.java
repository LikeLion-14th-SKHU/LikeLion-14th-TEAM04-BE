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
import com.memory_atelier.user.application.WithdrawalPolicy;
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
    private final WithdrawalPolicy withdrawalPolicy;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // 회원가입
    // 활성 유저가 있거나, 탈퇴한 유저라도 유예+쿨다운 기간이 안 지났으면 같은 이메일 가입을 막는다
    @Transactional
    public void signup(SignupRequestDto request) {

        if (userRepository.existsBlockingEmail(request.email(), withdrawalPolicy.emailBlockCutoff())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL, ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        // 쿨다운을 통과했어도 email 컬럼은 유니크 제약이라, 그 자리를 차지하고 있는 옛 탈퇴
        // 계정이 남아있으면 이메일을 비워 자리를 내준다(있다면 위 체크를 통과했다는 것 자체가
        // 쿨다운이 끝난 탈퇴 계정이라는 뜻이다). saveAndFlush로 즉시 반영해야 한다 — 안 그러면
        // Hibernate가 이 UPDATE보다 아래의 새 유저 INSERT를 먼저 내보내면서 유니크 제약에 걸린다
        userRepository.findByEmail(request.email()).ifPresent(oldUser -> {
            oldUser.releaseEmailForReuse();
            userRepository.saveAndFlush(oldUser);
        });

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

        // 탈퇴 유예 기간 안이면 비밀번호까지 확인된 이 로그인으로 탈퇴를 자동 취소한다.
        // 유예 기간이 지난 확정 탈퇴 계정은 탈퇴 여부를 노출하지 않기 위해 존재하지 않는
        // 계정과 동일하게(USER_NOT_FOUND) 처리한다
        if (!user.isActive()) {
            if (withdrawalPolicy.isWithinGracePeriod(user.getDeletedAt())) {
                user.cancelWithdrawal();
            } else {
                throw new CustomException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage());
            }
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

        // 탈퇴한 계정은 재발급으로 재활성화되지 않는다 — 재활성화는 비밀번호를 다시 확인하는
        // 로그인에서만 일어난다(login() 참고). 안 그러면 탈퇴해도 남아있는 refresh token으로
        // 계속 새 access token을 찍어내며 사실상 탈퇴가 무력화된다
        if (!user.isActive()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage());
        }

        String newAccessToken = jwtUtil.generateToken(user.getUserId(), user.getRole());

        return new TokenRefreshResponseDto(newAccessToken);
    }

}
