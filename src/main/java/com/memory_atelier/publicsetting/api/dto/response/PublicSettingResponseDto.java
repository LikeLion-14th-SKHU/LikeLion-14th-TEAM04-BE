package com.memory_atelier.publicsetting.api.dto.response;

import com.memory_atelier.publicsetting.domain.PublicSetting;
import io.swagger.v3.oas.annotations.media.Schema;

public record PublicSettingResponseDto(
        @Schema(description = "공개 설정 id", example = "1")
        Long publicSettingId,

        @Schema(description = "공개 대상 종류", example = "ALL_COLLECTION", allowableValues = {"ALL_COLLECTION", "SINGLE_CARD"})
        String targetType,

        @Schema(description = "SINGLE_CARD일 때만 값이 있는 콘셉트 id", example = "null")
        Long conceptId,

        @Schema(description = "공개 여부", example = "true")
        boolean isPublic,

        @Schema(description = "공유 토큰", example = "3f2e9c1a-1234-4a5b-9c1a-abcdef123456")
        String shareToken
) {
    public static PublicSettingResponseDto from(PublicSetting setting) {
        return new PublicSettingResponseDto(
                setting.getPublicSettingId(),
                setting.getTargetType().name(),
                setting.getConcept() != null ? setting.getConcept().getConceptId() : null,
                setting.isPublic(),
                setting.getShareToken());
    }
}
