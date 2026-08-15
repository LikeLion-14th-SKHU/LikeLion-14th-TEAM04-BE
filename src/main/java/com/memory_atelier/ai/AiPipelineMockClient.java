package com.memory_atelier.ai;

import com.memory_atelier.ai.dto.request.PipelineRunRequestDto;
import com.memory_atelier.ai.dto.response.JobAcceptedResponseDto;
import com.memory_atelier.ai.dto.response.JobInfoResponseDto;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AI 파이프라인 서버가 로컬에 없어도 다른 도메인(Memory, Edition Generation)이 개발·테스트할 수
 * 있도록 하는 가짜 클라이언트. {@code ai.pipeline.client=mock}일 때만 활성화된다.
 *
 * <p>상태 전이는 실제 서버와 동일하게 running → awaiting_selection → (select) → running → done을
 * 따르되, 각 running 구간은 {@code ai.pipeline.mock-poll-count}번 폴링하면 다음 상태로 넘어간다.
 */
@Component
@ConditionalOnProperty(name = "ai.pipeline.client", havingValue = "mock")
public class AiPipelineMockClient implements AiPipelineClient {

    private final Map<String, MockJob> jobs = new ConcurrentHashMap<>();
    private final int pollsPerPhase;

    public AiPipelineMockClient(@Value("${ai.pipeline.mock-poll-count:2}") int pollsPerPhase) {
        this.pollsPerPhase = pollsPerPhase;
    }

    @Override
    public JobAcceptedResponseDto runPipeline(PipelineRunRequestDto request) {
        String jobId = "mock-" + UUID.randomUUID();
        jobs.put(jobId, new MockJob());
        return new JobAcceptedResponseDto(jobId);
    }

    @Override
    public JobInfoResponseDto getJob(String jobId) {
        MockJob job = jobs.get(jobId);
        if (job == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 job입니다: " + jobId);
        }

        String now = Instant.now().toString();
        int polls = job.pollCount.getAndIncrement();

        if (!job.selected) {
            if (polls < pollsPerPhase) {
                return new JobInfoResponseDto(
                        jobId, JobInfoResponseDto.STATUS_RUNNING, "1", "옷 분석·사연 해석 중 (mock)",
                        null, null, now, now);
            }
            return new JobInfoResponseDto(
                    jobId, JobInfoResponseDto.STATUS_AWAITING_SELECTION, null,
                    "후보 중 1개를 선택하세요 (mock)",
                    Map.of("candidates", mockCandidates(jobId)),
                    null, now, now);
        }

        if (polls < pollsPerPhase) {
            return new JobInfoResponseDto(
                    jobId, JobInfoResponseDto.STATUS_RUNNING, "4", "3D 모델 생성 중 (mock)",
                    null, null, now, now);
        }
        return new JobInfoResponseDto(
                jobId, JobInfoResponseDto.STATUS_DONE, null, null,
                Map.of(
                        "glb_url", "https://mock.local/" + jobId + "/model.glb",
                        "front_image_url", "https://mock.local/" + jobId + "/front.png"),
                null, now, now);
    }

    @Override
    public JobAcceptedResponseDto selectCandidate(String jobId, int candidateIndex) {
        MockJob job = jobs.get(jobId);
        if (job == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 job입니다: " + jobId);
        }
        job.selected = true;
        job.pollCount.set(0);
        return new JobAcceptedResponseDto(jobId);
    }

    private List<Map<String, Object>> mockCandidates(String jobId) {
        return List.of(
                Map.of("index", 0, "image_url", "https://mock.local/" + jobId + "/safe.png"),
                Map.of("index", 1, "image_url", "https://mock.local/" + jobId + "/balanced.png"),
                Map.of("index", 2, "image_url", "https://mock.local/" + jobId + "/bold.png"));
    }

    private static final class MockJob {
        private final AtomicInteger pollCount = new AtomicInteger(0);
        private volatile boolean selected = false;
    }
}
