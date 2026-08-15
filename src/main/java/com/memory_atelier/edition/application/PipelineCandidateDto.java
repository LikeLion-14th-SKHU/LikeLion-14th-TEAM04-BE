package com.memory_atelier.edition.application;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * FastAPI {@code awaiting_selection} 응답의 {@code result.candidates[]} 항목 하나.
 * {@code JobInfoResponseDto.result()}가 순수 {@code Map}이라, 이 도메인에서 직접 이 모양으로
 * 변환(ObjectMapper#convertValue)해서 쓴다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PipelineCandidateDto(
        int index,
        Spec spec,
        String imageUrl,
        Gate gate
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Spec(
            String conceptName,
            String category,
            String categoryReason,
            String baseProduct,
            String riskProfile,
            Integer interventionLevel,
            String creationMethod,
            List<AppliedElementDto> appliedElements,
            String imagePrompt
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AppliedElementDto(
            String fromElement,
            String toElement,
            String reason
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Gate(
            boolean passed,
            int score,
            List<String> failReasons
    ) {
    }
}
