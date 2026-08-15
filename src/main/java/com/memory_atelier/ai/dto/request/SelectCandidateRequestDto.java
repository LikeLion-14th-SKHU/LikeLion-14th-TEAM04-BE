package com.memory_atelier.ai.dto.request;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// {@code POST /ai/v1/jobs/{job_id}/select} 요청 본문.
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SelectCandidateRequestDto(
        // {@code awaiting_selection} 상태 응답의 {@code result.candidates} 인덱스
        int candidateIndex
) {
}
