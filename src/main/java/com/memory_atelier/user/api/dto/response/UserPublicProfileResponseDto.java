package com.memory_atelier.user.api.dto.response;

import com.memory_atelier.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "회원 공개 프로필 정보 (이메일 등 민감 정보 제외)")
@Builder
public record UserPublicProfileResponseDto(
        @Schema(description = "회원 id", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "구름빵")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://memory-atelier.s3.ap-northeast-2.amazonaws.com/abc123_profile.png")
        String profileImageUrl
) {
    public static UserPublicProfileResponseDto from(User user) {
        return new UserPublicProfileResponseDto(
                user.getUserId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
