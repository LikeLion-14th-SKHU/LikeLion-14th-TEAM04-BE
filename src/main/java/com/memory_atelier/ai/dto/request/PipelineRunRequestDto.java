package com.memory_atelier.ai.dto.request;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// {@code POST /ai/v1/pipeline-run} 요청 본문
// FastAPI {@code PipelineRequest}와 1:1 대응(snake_case)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PipelineRunRequestDto(
        String imageBase64,
        UserInputDto userInput,
        String targetCategory,
        boolean usePro
) {
    public static PipelineRunRequestDto of(String imageBase64, UserInputDto userInput) {
        return new PipelineRunRequestDto(imageBase64, userInput, null, false);
    }
}
