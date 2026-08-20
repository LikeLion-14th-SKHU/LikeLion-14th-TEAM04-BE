package com.memory_atelier.community.api.dto.response;

import com.memory_atelier.global.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공유 링크로 열람하는 콘텐츠")
public record SharedViewResponseDto(
        @Schema(description = "공개 대상 종류", example = "ALL_COLLECTION", allowableValues = {"ALL_COLLECTION", "SINGLE_CARD"})
        String targetType,

        @Schema(description = "공유한 사용자 닉네임", example = "공개설정오너")
        String ownerNickname,

        @Schema(description = "공개된 전시 카드 페이지(SINGLE_CARD면 총 1개)")
        PageResponse<SharedCardResponseDto> cards
) {
}
