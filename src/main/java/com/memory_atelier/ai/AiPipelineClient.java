package com.memory_atelier.ai;

import com.memory_atelier.ai.dto.request.PipelineRunRequestDto;
import com.memory_atelier.ai.dto.response.JobAcceptedResponseDto;
import com.memory_atelier.ai.dto.response.JobInfoResponseDto;

// FastAPI(AI 파이프라인 서버)의 job 기반 비동기 API 호출 창구
// 콜백을 지원하지 않으므로, 진행 상황은 {@link #getJob(String)}을 호출하는 쪽에서 직접 폴링해야 한다
public interface AiPipelineClient {

    // {@code POST /ai/v1/pipeline-run} — 파이프라인 실행을 접수하고 job id를 받는다(202)
    JobAcceptedResponseDto runPipeline(PipelineRunRequestDto request);

    // {@code GET /ai/v1/jobs/{jobId}} — job의 현재 상태를 조회한다
    JobInfoResponseDto getJob(String jobId);

    // {@code POST /ai/v1/jobs/{jobId}/select} — {@code awaiting_selection} 상태에서 사용자가 고른 콘셉트 후보로 파이프라인(3D 변환 이후 단계)을 재개한다(202)
    JobAcceptedResponseDto selectCandidate(String jobId, int candidateIndex);
}
