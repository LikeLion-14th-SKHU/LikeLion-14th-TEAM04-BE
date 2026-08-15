package com.memory_atelier.auth.api;

import com.memory_atelier.auth.application.AuthService;
import com.memory_atelier.auth.api.dto.request.LoginRequestDto;
import com.memory_atelier.auth.api.dto.request.SignupRequestDto;
import com.memory_atelier.auth.api.dto.request.TokenRefreshRequestDto;
import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.auth.api.dto.response.TokenRefreshResponseDto;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "로그인 API", description = "회원가입, 로그인 관련 API")
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody @Valid SignupRequestDto request) {
        authService.signup(request);
        return ApiResponse.successEmpty(SuccessCode.SIGNUP_SUCCESS);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, response);
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenRefreshResponseDto>> refresh(
            @RequestBody @Valid TokenRefreshRequestDto request) {
        TokenRefreshResponseDto response = authService.refresh(request);
        return ApiResponse.success(SuccessCode.TOKEN_REFRESH_SUCCESS, response);
    }

}