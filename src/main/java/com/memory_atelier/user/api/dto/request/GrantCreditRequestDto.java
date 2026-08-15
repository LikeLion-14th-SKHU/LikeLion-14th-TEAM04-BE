package com.memory_atelier.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "크레딧 지급 요청")
public record GrantCreditRequestDto(
        @Schema(description = "지급할 크레딧 수량(1 이상)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "크레딧 수량은 1 이상이어야 합니다.")
        int amount
) {
}
