package com.memory_atelier.user.api.dto.response;

import com.memory_atelier.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "회원 상세 정보(본인 조회 또는 관리자 조회 응답)")
@Builder
public record UserInfoResponseDto(
        @Schema(description = "회원 id", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "닉네임", example = "구름빵")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://memory-atelier.s3.ap-northeast-2.amazonaws.com/abc123_profile.png")
        String profileImageUrl,

        @Schema(description = "이메일 인증 여부", example = "false")
        boolean emailVerified,

        @Schema(description = "권한", example = "USER", allowableValues = {"USER", "ADMIN"})
        String role
) {
    public static UserInfoResponseDto from(User user)
    {
        return new UserInfoResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.isEmailVerified(),
                user.getRole().name()
        );

    }
}
