package com.memory_atelier.naver.api;

import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.naver.application.NaverOAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/naver")
@Tag(name = "네이버 로그인 API", description = "네이버 소셜 로그인 API")
public class NaverOAuthController {

    private final NaverOAuthService naverOAuthService;

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<LoginResponseDto>> naverCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        LoginResponseDto response = naverOAuthService.naverLogin(code, state);
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, response);
    }
}