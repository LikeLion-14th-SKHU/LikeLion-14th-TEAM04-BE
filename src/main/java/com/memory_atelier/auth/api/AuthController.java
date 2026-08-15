package com.memory_atelier.auth.api;

import com.memory_atelier.auth.application.AuthService;
import com.memory_atelier.auth.api.dto.request.LoginRequestDto;
import com.memory_atelier.auth.api.dto.request.SignupRequestDto;
import com.memory_atelier.auth.api.dto.request.TokenRefreshRequestDto;
import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.auth.api.dto.response.TokenRefreshResponseDto;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/signup")
    @Operation(
            summary = "로컬 회원가입",
            description = "이메일/비밀번호로 신규 계정을 생성합니다. 이미 가입된 이메일이면 실패합니다. 인증 없이 호출 가능합니다."
    )
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody @Valid SignupRequestDto request) {
        authService.signup(request);
        return ApiResponse.successEmpty(SuccessCode.SIGNUP_SUCCESS);
    }

    @PostMapping("/login")
    @Operation(
            summary = "로컬 로그인",
            description = "이메일/비밀번호로 로그인하고 액세스·리프레시 토큰을 발급합니다. 인증 없이 호출 가능합니다."
    )
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, response);
    }

    @PostMapping("/reissue")
    @Operation(
            summary = "액세스 토큰 재발급",
            description = "만료되지 않은 리프레시 토큰으로 새 액세스 토큰을 발급합니다. 리프레시 토큰 자체는 재발급되지 않습니다(로테이션 없음). "
                    + "인증 없이 호출 가능합니다."
    )
    public ResponseEntity<ApiResponse<TokenRefreshResponseDto>> refresh(
            @RequestBody @Valid TokenRefreshRequestDto request) {
        TokenRefreshResponseDto response = authService.refresh(request);
        return ApiResponse.success(SuccessCode.TOKEN_REFRESH_SUCCESS, response);
    }

}
