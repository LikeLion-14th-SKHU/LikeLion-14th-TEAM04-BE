package com.memory_atelier.ai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.Map;

// {@code GET /ai/v1/jobs/{job_id}} 응답. FastAPI {@code JobInfo}와 1:1 대응.
// {@code result}는 상태에 따라 모양이 달라서(대기 시 candidates[], 완료 시 최종 산출물)
// 이 인프라 계층에서는 구조화하지 않고 {@code Map}으로 그대로 넘긴다 — 파싱은 이걸 쓰는 도메인이 담당
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JobInfoResponseDto(
        String jobId,
        String status,
        String stage,
        String detail,
        Map<String, Object> result,
        String error,
        String createdAt,
        String updatedAt
) {
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_AWAITING_SELECTION = "awaiting_selection";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_FAILED = "failed";

    public boolean isDone() {
        return STATUS_DONE.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    public boolean isAwaitingSelection() {
        return STATUS_AWAITING_SELECTION.equals(status);
    }
}
