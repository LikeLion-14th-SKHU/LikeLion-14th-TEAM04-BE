package com.memory_atelier.google.api;

import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.google.application.GoogleOAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/google")
@Tag(name = "구글 로그인 API", description = "구글 소셜 로그인 API")
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<LoginResponseDto>> googleCallback(@RequestParam("code") String code) {
        LoginResponseDto response = googleOAuthService.googleLogin(code);
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, response);
    }
}
