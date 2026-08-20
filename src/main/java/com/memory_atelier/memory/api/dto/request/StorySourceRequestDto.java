package com.memory_atelier.memory.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record StorySourceRequestDto(
        @Schema(description = "에디션 생성 시 AI로 다듬은 사연을 사용할지 여부. false면 원문을 사용합니다.", example = "true")
        boolean useRefinedStory
) {
}
