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

// AI 파이프라인 서버가 로컬에 없어도 다른 도메인(Memory, Edition Generation)이 개발·테스트할 수 있도록 하는 가짜 클라이언트
// {@code ai.pipeline.client=mock}일 때만 활성화된다
// 상태 전이는 실제 서버와 동일하게 running → awaiting_selection → (select) → running → done을 따르되,
// 각 running 구간은 {@code ai.pipeline.mock-poll-count}번 폴링하면 다음 상태로 넘어간다
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
                    Map.of(
                            "analysis", Map.of(
                                    "edition_name_candidates", List.of("Mock Heritage Edition", "Mock Memory Edition"),
                                    "certificate_text", "mock 보증서 문구"),
                            "candidates", mockCandidates(jobId)),
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
                        "front_image_url", "https://mock.local/" + jobId + "/front.png",
                        "curation", Map.of("recommendations", mockRecommendations())),
                null, now, now);
    }

    // 3D 변환과 같은 job이 이어서 만들어 주는 Stage 5(큐레이션) mock — 실제 AI 서버도
    // run_after_selection에서 이 두 산출물을 같은 done 응답에 함께 담아 준다
    private List<Map<String, Object>> mockRecommendations() {
        return List.of(
                Map.of(
                        "product_id", "mock-product-1",
                        "name_kr", "목업 크로스백",
                        "reason", "mock 큐레이션 이유 1",
                        "tagline", "mock 태그라인 1",
                        "image_url", "https://mock.local/products/mock-product-1.png"),
                Map.of(
                        "product_id", "mock-product-2",
                        "name_kr", "목업 파우치",
                        "reason", "mock 큐레이션 이유 2",
                        "tagline", "mock 태그라인 2",
                        "image_url", "https://mock.local/products/mock-product-2.png"));
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

    private static final List<String> RISK_PROFILES = List.of("safe", "balanced", "bold");
    private static final List<Integer> MOCK_SCORES = List.of(82, 95, 88); // 동점 처리 확인용으로 순서를 일부러 섞음

    private List<Map<String, Object>> mockCandidates(String jobId) {
        return List.of(
                mockCandidate(jobId, 0),
                mockCandidate(jobId, 1),
                mockCandidate(jobId, 2));
    }

    private Map<String, Object> mockCandidate(String jobId, int index) {
        String riskProfile = RISK_PROFILES.get(index);
        // category_reason은 실제 응답에서 null일 수 있는 필드라 Map.of()(null 값 금지)로는 못 만든다.
        Map<String, Object> spec = new java.util.HashMap<>();
        spec.put("concept_name", "Mock " + riskProfile + " 컨셉");
        spec.put("category", "가방");
        spec.put("category_reason", null);
        spec.put("base_product", "mock-product-" + index);
        spec.put("risk_profile", riskProfile);
        spec.put("intervention_level", index + 1);
        spec.put("creation_method", "mock 재창조 방식");
        spec.put("applied_elements", List.of(Map.of(
                "from_element", "옷의 패턴",
                "to_element", "제품 플랩",
                "reason", "mock 근거")));
        spec.put("image_prompt", "mock prompt");

        return Map.of(
                "index", index,
                "spec", spec,
                "image_url", "https://mock.local/" + jobId + "/" + riskProfile + ".png",
                "gate", Map.of("passed", true, "score", MOCK_SCORES.get(index), "fail_reasons", List.of()));
    }

    private static final class MockJob {
        private final AtomicInteger pollCount = new AtomicInteger(0);
        private volatile boolean selected = false;
    }
}
