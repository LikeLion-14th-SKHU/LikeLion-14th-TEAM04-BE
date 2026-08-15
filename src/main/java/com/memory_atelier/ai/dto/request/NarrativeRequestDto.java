package com.memory_atelier.ai.dto.request;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * {@code POST /ai/v1/narrative}(Stage 1, 동기) 요청 본문.
 * {@code imageBase64}는 null 허용 — 없으면 텍스트만으로 분석한다(비전 분석 없이 사연만 재해석할 때 사용).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NarrativeRequestDto(
        String imageBase64,
        UserInputDto userInput
) {
}
