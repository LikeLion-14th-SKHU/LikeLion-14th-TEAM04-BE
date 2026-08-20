package com.memory_atelier.edition.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "에디션명 선택 요청")
public record EditionNameRequestDto(
        @Schema(description = "AI 후보 중 하나를 그대로 쓰거나 직접 지은 이름", example = "MCM Heritage Edition", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "에디션명은 필수입니다.")
        @Size(max = 50, message = "에디션명은 50자를 넘을 수 없습니다.")
        String editionName
) {
}
