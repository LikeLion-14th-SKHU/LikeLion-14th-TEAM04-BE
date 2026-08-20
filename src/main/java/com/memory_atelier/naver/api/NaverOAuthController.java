package com.memory_atelier.naver.api;

import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.global.oauth.OAuthRedirectHelper;
import com.memory_atelier.naver.application.NaverOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final OAuthRedirectHelper oAuthRedirectHelper;

    @GetMapping("/callback")
    @Operation(
            summary = "네이버 로그인 콜백",
            description = "네이버 인가(authorize) 화면에서 로그인 동의 후 리다이렉트되며 호출되는 콜백입니다. "
                    + "`code`는 직접 입력하는 값이 아니라 네이버가 발급하는 1회용 값이고, `state`는 authorize 요청 시 보낸 값과 "
                    + "동일해야 합니다. 브라우저에서 네이버 OAuth authorize URL로 로그인한 뒤 자동으로 이 API가 호출됩니다. "
                    + "가입 이력이 없으면 신규 가입 후 로그인 처리됩니다. 인증 없이 호출 가능합니다."
                    + "처리 완료 후 프론트엔드로 302 리다이렉트되며, 쿼리 파라미터로 accessToken과"
                    + "refreshToken이 전달됩니다."
    )
    public ResponseEntity<Void> naverCallback(
            @Parameter(description = "네이버가 발급한 1회용 인가 코드", example = "abcd1234...")
            @RequestParam("code") String code,
            @Parameter(description = "authorize 요청 시 보낸 state 값과 동일해야 함(CSRF 방지)", example = "test1234")
            @RequestParam("state") String state
    ) {
        LoginResponseDto response = naverOAuthService.naverLogin(code, state);
        return oAuthRedirectHelper.redirectWithTokens(response);
    }
}
