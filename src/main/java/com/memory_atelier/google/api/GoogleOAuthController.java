package com.memory_atelier.google.api;

import com.memory_atelier.auth.api.dto.response.LoginResponseDto;
import com.memory_atelier.global.oauth.OAuthRedirectHelper;
import com.memory_atelier.google.application.GoogleOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final OAuthRedirectHelper oAuthRedirectHelper;

    @GetMapping("/callback")
    @Operation(
            summary = "구글 로그인 콜백",
            description = "구글 인가(authorize) 화면에서 로그인 동의 후 리다이렉트되며 호출되는 콜백입니다. "
                    + "`code`는 직접 입력하는 값이 아니라 구글이 발급하는 1회용 값으로, "
                    + "브라우저에서 구글 OAuth authorize URL로 로그인한 뒤 자동으로 이 API가 호출됩니다. "
                    + "가입 이력이 없으면 신규 가입 후 로그인 처리됩니다. 인증 없이 호출 가능합니다."
                    + "처리 완료 후 프론트엔드로 302 리다이렉트되며, 쿼리 파라미터로 accessToken과"
                    + "refreshToken이 전달됩니다."
    )
    public ResponseEntity<Void> googleCallback(
            @Parameter(description = "구글이 발급한 1회용 인가 코드", example = "4/0AY0e-g7...")
            @RequestParam("code") String code) {
        LoginResponseDto response = googleOAuthService.googleLogin(code);
        return oAuthRedirectHelper.redirectWithTokens(response);
    }
}
