package com.memory_atelier.kakao.api;

import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.kakao.application.KakaoOAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
@Tag(name = "카카오 로그인 API", description = "카카오 소셜 로그인 API")
public class KakaoOAuthController {

    private final KakaoOAuthService kakaoOAuthService;

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<LoginResponseDto>> kakaoCallback(@RequestParam("code") String code) {
        LoginResponseDto response = kakaoOAuthService.kakaoLogin(code);
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, response);
    }
}