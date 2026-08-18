package com.memory_atelier.certificate.api.dto.request;

import com.memory_atelier.user.domain.CollectionTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "컬렉션 테마 변경 요청")
public record CollectionThemeRequestDto(
        @Schema(description = "적용할 테마", example = "IVORY",
                allowableValues = {"WHITE", "IVORY", "BUTTER", "OLIVE_CREAM", "DUSTY_ROSE", "LIGHT_MOCHA"})
        @NotNull(message = "테마는 필수입니다.")
        CollectionTheme theme
) {
}
