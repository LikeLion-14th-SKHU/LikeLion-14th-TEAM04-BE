package com.memory_atelier.ai;

import com.memory_atelier.ai.dto.request.PipelineRunRequestDto;
import com.memory_atelier.ai.dto.request.SelectCandidateRequestDto;
import com.memory_atelier.ai.dto.response.JobAcceptedResponseDto;
import com.memory_atelier.ai.dto.response.JobInfoResponseDto;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * FastAPI(AI 파이프라인 서버)를 실제 HTTP로 호출하는 구현체. 기본 구현체이며,
 * {@code ai.pipeline.client=mock}일 때는 {@link AiPipelineMockClient}로 대체된다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.pipeline.client", havingValue = "http", matchIfMissing = true)
public class AiPipelineHttpClient implements AiPipelineClient {

    private final RestClient aiRestClient;

    @Override
    public JobAcceptedResponseDto runPipeline(PipelineRunRequestDto request) {
        try {
            return aiRestClient.post()
                    .uri("/ai/v1/pipeline-run")
                    .body(request)
                    .retrieve()
                    .body(JobAcceptedResponseDto.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "AI 파이프라인 실행 요청에 실패했습니다.");
        }
    }

    @Override
    public JobInfoResponseDto getJob(String jobId) {
        try {
            return aiRestClient.get()
                    .uri("/ai/v1/jobs/{jobId}", jobId)
                    .retrieve()
                    .body(JobInfoResponseDto.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "AI 작업 상태 조회에 실패했습니다.");
        }
    }

    @Override
    public JobAcceptedResponseDto selectCandidate(String jobId, int candidateIndex) {
        try {
            return aiRestClient.post()
                    .uri("/ai/v1/jobs/{jobId}/select", jobId)
                    .body(new SelectCandidateRequestDto(candidateIndex))
                    .retrieve()
                    .body(JobAcceptedResponseDto.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "AI 콘셉트 후보 선택 요청에 실패했습니다.");
        }
    }
}
