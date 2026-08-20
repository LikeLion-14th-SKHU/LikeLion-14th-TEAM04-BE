package com.memory_atelier.certificate.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "컬렉션 이름 변경 요청")
public record CollectionNameRequestDto(
        @Schema(description = "변경할 컬렉션 이름", example = "구름이의 옷장", maxLength = 30)
        @NotBlank(message = "컬렉션 이름은 필수입니다.")
        @Size(max = 30, message = "컬렉션 이름은 30자 이하여야 합니다.")
        String collectionName
) {
}
