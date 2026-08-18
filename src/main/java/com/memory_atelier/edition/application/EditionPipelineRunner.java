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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

// 생성 배치 하나를 끝까지 돌린다
// 외부 호출(파이프라인 실행·폴링)은 트랜잭션 밖에서 하고, DB 반영만 {@link EditionPipelineStore}로 짧게 끊어 커밋한다
// 여기서 예외를 밖으로 던지지 않는다
// 호출자가 비동기 스레드라 받을 사람이 없고, 사용자에게는 예외가 아니라 콘셉트 상태로 실패를 알려야 하기 때문이다
@Slf4j
@Component
@RequiredArgsConstructor
public class EditionPipelineRunner {

    private final AiPipelineClient aiClient;
    private final AiImageFetcher imageFetcher;
    private final EditionPipelineStore store;
    private final ApplicationEventPublisher eventPublisher;
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
                    aiClient.runPipeline(PipelineRunRequestDto.of(imageBase64, userInput, input.targetCategory()));
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

    // 확정된 콘셉트 한 장의 3D 변환
    // 우리 AI 서버는 별도 3D 전용 엔드포인트가 없고, {@code awaiting_selection} 상태에서 멈춰 있던 job을 {@code selectCandidate}로 재개하면 이어서 3D까지 진행한다
    // 실패해도 콘셉트 상태는 건드리지 않는다
    // 카드는 2D 콘셉트 이미지로 계속 보여줄 수 있어서 "실패"로 확정할 이유가 없다
    @Async("editionPipelineExecutor")
    public void runModelConversion(Long conceptId) {
        try {
            EditionPipelineStore.ModelConversionInput input = store.loadModelConversionInput(conceptId);
            aiClient.selectCandidate(input.jobId(), input.candidateIndex());
            pollUntilModelDone(conceptId, input.jobId());
        } catch (RuntimeException e) {
            log.error("3D 모델 변환 실패: conceptId={}", conceptId, e);
        }
    }

    private void pollUntilModelDone(Long conceptId, String jobId) {
        for (int attempt = 0; attempt < maxPollAttempts; attempt++) {
            sleep();

            JobInfoResponseDto job = aiClient.getJob(jobId);
            if (job.isDone()) {
                applyModelResult(conceptId, job);
                return;
            }
            if (job.isFailed()) {
                log.warn("3D 변환 파이프라인이 실패 상태를 반환함: conceptId={}, error={}", conceptId, job.error());
                return;
            }
            // running이면 계속 폴링
        }
        log.warn("3D 변환 폴링 시간 초과: conceptId={}, jobId={}", conceptId, jobId);
    }

    private void applyModelResult(Long conceptId, JobInfoResponseDto job) {
        Map<String, Object> result = job.result();
        if (result == null) {
            return;
        }
        String modelUrl = (String) result.get("glb_url");
        String frontImageUrl = (String) result.get("front_image_url");
        store.applyModel(conceptId, modelUrl, frontImageUrl);

        // 같은 job이 3D 변환 직후 Stage 5(큐레이션)까지 이어서 실행해 result.curation에 실어 준다
        // (AI 저장소 ai_pipeline/api/endpoints/full_pipeline.py의 run_after_selection 참고) —
        // 우리가 /ai/v1/curation을 따로 호출할 필요가 없다.
        applyCuration(conceptId, result);
    }

    @SuppressWarnings("unchecked")
    private void applyCuration(Long conceptId, Map<String, Object> result) {
        Map<String, Object> curation = (Map<String, Object>) result.get("curation");
        if (curation == null) {
            return;
        }
        List<Map<String, Object>> rawRecommendations =
                (List<Map<String, Object>>) curation.getOrDefault("recommendations", List.of());
        List<PipelineEvents.CurationReceived.RecommendationPayload> recommendations = rawRecommendations.stream()
                .map(r -> new PipelineEvents.CurationReceived.RecommendationPayload(
                        (String) r.get("product_id"),
                        (String) r.get("name_kr"),
                        (String) r.get("reason"),
                        (String) r.get("tagline"),
                        (String) r.get("image_url")))
                .toList();
        eventPublisher.publishEvent(new PipelineEvents.CurationReceived(conceptId, recommendations));
    }
}
