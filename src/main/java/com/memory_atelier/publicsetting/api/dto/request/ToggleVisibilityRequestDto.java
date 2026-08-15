package com.memory_atelier.publicsetting.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ToggleVisibilityRequestDto(
        @Schema(description = "공개 여부", example = "true")
        boolean isPublic
) {
}
