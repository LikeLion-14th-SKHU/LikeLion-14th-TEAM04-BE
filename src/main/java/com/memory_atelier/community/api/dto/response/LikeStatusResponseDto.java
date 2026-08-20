package com.memory_atelier.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LikeStatusResponseDto(
        @Schema(description = "좋아요 수", example = "12")
        long likeCount,

        @Schema(description = "내가 좋아요 눌렀는지 여부", example = "true")
        boolean likedByMe
) {
}
