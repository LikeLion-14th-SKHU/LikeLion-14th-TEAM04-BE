package com.memory_atelier.kakao.api;

import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.kakao.application.KakaoOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(
            summary = "카카오 로그인 콜백",
            description = "카카오 인가(authorize) 화면에서 로그인 동의 후 리다이렉트되며 호출되는 콜백입니다. "
                    + "`code`는 직접 입력하는 값이 아니라 카카오가 발급하는 1회용 값으로, "
                    + "브라우저에서 카카오 OAuth authorize URL로 로그인한 뒤 자동으로 이 API가 호출됩니다. "
                    + "가입 이력이 없으면 신규 가입 후 로그인 처리됩니다. 인증 없이 호출 가능합니다."
    )
    public ResponseEntity<ApiResponse<LoginResponseDto>> kakaoCallback(
            @Parameter(description = "카카오가 발급한 1회용 인가 코드", example = "abcd1234...")
            @RequestParam("code") String code) {
        LoginResponseDto response = kakaoOAuthService.kakaoLogin(code);
        return ApiResponse.success(SuccessCode.LOGIN_SUCCESS, response);
    }
}
