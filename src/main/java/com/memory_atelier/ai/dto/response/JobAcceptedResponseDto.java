package com.memory_atelier.ai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// {@code POST /ai/v1/pipeline-run}, {@code POST /ai/v1/jobs/{job_id}/select} 공통 응답(202)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JobAcceptedResponseDto(
        String jobId
) {
}
