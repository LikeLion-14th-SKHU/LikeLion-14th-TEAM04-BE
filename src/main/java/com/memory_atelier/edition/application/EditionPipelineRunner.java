package com.memory_atelier.edition.application;

import com.memory_atelier.ai.AiImageFetcher;
import com.memory_atelier.ai.AiPipelineClient;
import com.memory_atelier.ai.dto.request.PipelineRunRequestDto;
import com.memory_atelier.ai.dto.request.UserInputDto;
import com.memory_atelier.ai.dto.response.JobAcceptedResponseDto;
import com.memory_atelier.ai.dto.response.JobInfoResponseDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 생성 배치 하나를 끝까지 돌린다. 외부 호출(파이프라인 실행·폴링)은 트랜잭션 밖에서 하고,
 * DB 반영만 {@link EditionPipelineStore}로 짧게 끊어 커밋한다.
 *
 * <p>여기서 예외를 밖으로 던지지 않는다 — 호출자가 비동기 스레드라 받을 사람이 없고, 사용자에게는
 * 예외가 아니라 콘셉트 상태로 실패를 알려야 하기 때문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EditionPipelineRunner {

    private final AiPipelineClient aiClient;
    private final AiImageFetcher imageFetcher;
    private final EditionPipelineStore store;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.poll-interval-ms:2000}")
    private long pollIntervalMs;

    @Value("${app.ai.max-poll-attempts:60}")
    private int maxPollAttempts;

    @Async("editionPipelineExecutor")
    public void runGeneration(Long generationId) {
        try {
            EditionPipelineStore.PipelineInput input = store.loadInput(generationId);
            String imageBase64 = imageFetcher.fetchAsBase64(input.photoUrl());
            UserInputDto userInput = new UserInputDto(
                    new UserInputDto.ClothingCategoryDto(input.categoryMain(), input.categorySub()),
                    input.material(),
                    input.condition(),
                    input.story());

            JobAcceptedResponseDto accepted =
                    aiClient.runPipeline(PipelineRunRequestDto.of(imageBase64, userInput));
            store.assignJobId(generationId, accepted.jobId());

            pollUntilAwaitingSelection(generationId, accepted.jobId());
        } catch (RuntimeException e) {
            log.error("에디션 생성 파이프라인 실패: generationId={}", generationId, e);
            store.failAllConcepts(generationId);
        }
    }

    private void pollUntilAwaitingSelection(Long generationId, String jobId) {
        for (int attempt = 0; attempt < maxPollAttempts; attempt++) {
            sleep();

            JobInfoResponseDto job = aiClient.getJob(jobId);
            if (job.isAwaitingSelection()) {
                applyResult(generationId, job);
                return;
            }
            if (job.isFailed()) {
                log.warn("에디션 생성 파이프라인이 실패 상태를 반환함: generationId={}, error={}", generationId, job.error());
                store.failAllConcepts(generationId);
                return;
            }
            // running이면 계속 폴링
        }
        log.warn("에디션 생성 파이프라인 폴링 시간 초과: generationId={}, jobId={}", generationId, jobId);
        store.failAllConcepts(generationId);
    }

    @SuppressWarnings("unchecked")
    private void applyResult(Long generationId, JobInfoResponseDto job) {
        Map<String, Object> result = job.result();
        if (result == null) {
            store.failAllConcepts(generationId);
            return;
        }

        // edition_name_candidates/certificate_text는 result 최상위가 아니라
        // result.analysis(Stage 1 AnalysisResult) 안에 들어있다.
        Map<String, Object> analysis = (Map<String, Object>) result.get("analysis");
        List<String> editionNameCandidates = (analysis != null)
                ? objectMapper.convertValue(analysis.get("edition_name_candidates"), List.class)
                : List.of();
        String certificateText = (analysis != null) ? (String) analysis.get("certificate_text") : null;

        List<Map<String, Object>> rawCandidates =
                (List<Map<String, Object>>) result.getOrDefault("candidates", List.of());
        List<PipelineCandidateDto> candidates = rawCandidates.stream()
                .map(c -> objectMapper.convertValue(c, PipelineCandidateDto.class))
                .toList();

        store.applyAwaitingSelection(generationId, editionNameCandidates, certificateText, candidates);
    }

    private void sleep() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
