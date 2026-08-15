package com.memory_atelier.edition.application;

import com.memory_atelier.edition.domain.AppliedElement;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.edition.domain.repository.EditionGenerationRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.memory.domain.ItemOptions;
import com.memory_atelier.memory.domain.Memory;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 파이프라인 실행 중 DB 반영만 짧게 끊어 커밋하는 곳. 외부 호출(AI 서버, 최대 수십 초)은 여기
 * 밖에서 하고, 결과 반영만 {@code REQUIRES_NEW}로 짧게 커밋한다 — 긴 호출을 트랜잭션으로 감싸면
 * 그동안 DB 커넥션이 묶여 커넥션 풀이 금방 마른다.
 */
@Component
@RequiredArgsConstructor
public class EditionPipelineStore {

    private final EditionGenerationRepository generationRepository;
    private final EditionConceptRepository conceptRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assignJobId(Long generationId, String jobId) {
        getGeneration(generationId).assignJobId(jobId);
    }

    /** 파이프라인 호출에 필요한 값만 뽑아서 넘긴다 — 트랜잭션 밖에서 지연 로딩에 걸리지 않도록. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public PipelineInput loadInput(Long generationId) {
        EditionGeneration generation = getGeneration(generationId);
        Memory memory = generation.getMemory();
        String material = memory.isMaterialUnknown() ? ItemOptions.MATERIAL_UNSELECTED : memory.getMaterialUser();
        return new PipelineInput(
                memory.getPhotoUrl(),
                memory.getCategoryMain(),
                memory.getCategorySub(),
                material,
                memory.getConditionTags(),
                generation.getStorySnapshot());
    }

    public record PipelineInput(
            String photoUrl,
            String categoryMain,
            String categorySub,
            String material,
            List<String> condition,
            String story) {
    }

    /**
     * FastAPI가 {@code awaiting_selection}이 됐을 때의 결과 반영. 3장 다 실패했을 수도 있어서
     * candidates가 비어 있을 수 있다 — 그 경우 전부 실패 처리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyAwaitingSelection(
            Long generationId,
            List<String> editionNameCandidates,
            String certificateText,
            List<PipelineCandidateDto> candidates) {
        EditionGeneration generation = getGeneration(generationId);
        generation.applyNarrative(editionNameCandidates, certificateText);

        List<EditionConcept> concepts =
                conceptRepository.findAllByGenerationGenerationIdOrderByDisplayOrder(generationId);

        for (PipelineCandidateDto candidate : candidates) {
            if (candidate.index() < 0 || candidate.index() >= concepts.size()) {
                continue;
            }
            EditionConcept concept = concepts.get(candidate.index());
            PipelineCandidateDto.Spec spec = candidate.spec();
            concept.applyConceptImage(
                    spec.category(),
                    spec.categoryReason(),
                    spec.conceptName(),
                    spec.riskProfile(),
                    spec.interventionLevel(),
                    spec.baseProduct(),
                    spec.creationMethod(),
                    toAppliedElements(spec.appliedElements()),
                    candidate.imageUrl(),
                    candidate.gate().score());
        }
        // 후보에 없는(게이트 불합격 등으로 최종 산출물이 안 나온) 콘셉트는 실패 처리한다.
        for (EditionConcept concept : concepts) {
            if (concept.getStatus().hasImage()) {
                continue;
            }
            concept.markFailed();
        }

        unlockBestConcept(concepts);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failAllConcepts(Long generationId) {
        conceptRepository.findAllByGenerationGenerationIdOrderByDisplayOrder(generationId)
                .forEach(EditionConcept::markFailed);
    }

    /** 이미지가 나온 콘셉트 중 품질 점수가 가장 높은 1장만 무료로 연다. 동점이면 순번이 빠른 쪽. */
    private void unlockBestConcept(List<EditionConcept> concepts) {
        concepts.stream()
                .filter(c -> c.getStatus().hasImage())
                .max(Comparator.comparingInt(EditionConcept::getQualityScore)
                        .thenComparing(Comparator.comparingInt(EditionConcept::getDisplayOrder).reversed()))
                .ifPresent(EditionConcept::unlock);
    }

    private static List<AppliedElement> toAppliedElements(List<PipelineCandidateDto.AppliedElementDto> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream()
                .map(e -> new AppliedElement(e.fromElement(), e.toElement(), e.reason()))
                .toList();
    }

    private EditionGeneration getGeneration(Long generationId) {
        return generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.EDITION_NOT_FOUND));
    }
}
