package com.memory_atelier.community.api.dto.response;

import com.memory_atelier.publicsetting.domain.PublicSetting;
import io.swagger.v3.oas.annotations.media.Schema;

// 공개 옷장 목록의 한 사람. 카드가 아니라 사람이 단위다
// "누구의 옷장을 구경할지" 고르는 화면용이기 때문
// 카드를 둘러보는 피드는 EditionFeedItemResponseDto 쪽이다
@Schema(description = "공개 옷장(사용자) 한 명")
public record PublicCollectionResponseDto(
        @Schema(description = "사용자 id", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "공개설정오너")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "공유 토큰(공개 옷장 접근용)", example = "3f2e9c1a-1234-4a5b-9c1a-abcdef123456")
        String shareToken
) {
    public static PublicCollectionResponseDto from(PublicSetting setting) {
        return new PublicCollectionResponseDto(
                setting.getUser().getUserId(),
                setting.getUser().getNickname(),
                setting.getUser().getProfileImageUrl(),
                setting.getShareToken());
    }
}
