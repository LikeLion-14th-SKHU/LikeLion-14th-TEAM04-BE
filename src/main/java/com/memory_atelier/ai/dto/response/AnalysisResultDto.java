package com.memory_atelier.ai.dto.response;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * {@code POST /ai/v1/narrative} 응답. FastAPI {@code AnalysisResult}와 1:1 대응
 * (`ai_pipeline/schemas/analysis.py`).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AnalysisResultDto(
        Visual visual,
        Story story,
        List<String> editionNameCandidates,
        String certificateText
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Visual(
            List<String> colorPalette,
            String pattern,
            String materialUser,
            String materialEstimate,
            String materialFinal,
            List<String> conditionCues,
            List<String> vibeKeywords
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Story(
            String polished,
            String interpretation,
            List<String> emotionKeywords
    ) {
    }
}
