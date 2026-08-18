package com.memory_atelier.edition.application;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.TargetCategoryOptions;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.edition.domain.repository.EditionGenerationRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.memory.application.MemoryService;
import com.memory_atelier.memory.domain.Memory;
import com.memory_atelier.user.application.CreditPolicy;
import com.memory_atelier.user.application.CreditService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EditionGenerationService {

    private static final int CONCEPTS_PER_GENERATION = 3;

    private final EditionGenerationRepository generationRepository;
    private final EditionConceptRepository conceptRepository;
    private final MemoryService memoryService;
    private final CreditService creditService;
    private final CreditPolicy creditPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final int freeGenerationLimit;

    public EditionGenerationService(
            EditionGenerationRepository generationRepository,
            EditionConceptRepository conceptRepository,
            MemoryService memoryService,
            CreditService creditService,
            CreditPolicy creditPolicy,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.edition.free-generation-limit}") int freeGenerationLimit) {
        this.generationRepository = generationRepository;
        this.conceptRepository = conceptRepository;
        this.memoryService = memoryService;
        this.creditService = creditService;
        this.creditPolicy = creditPolicy;
        this.eventPublisher = eventPublisher;
        this.freeGenerationLimit = freeGenerationLimit;
    }

    /**
     * 생성 배치를 <b>접수</b>한다. 실제 파이프라인은 커밋 후 비동기로 돈다 — Stage 3(수 초)에
     * Stage 4까지 붙으면 HTTP 요청 하나가 오래 커넥션을 잡게 되므로, 여기서는 콘셉트 3행을
     * PENDING으로 만들어 두기만 하고 결과는 폴링으로 가져가게 한다.
     */
    @Transactional
    public Generated generate(Long userId, Long memoryId, String targetCategoryMain, String targetCategorySub) {
        TargetCategoryOptions.validate(targetCategoryMain, targetCategorySub);

        Memory memory = memoryService.getMemory(userId, memoryId);
        if (!memory.isAnalyzed()) {
            throw new CustomException(ErrorCode.MEMORY_ANALYSIS_NOT_READY);
        }

        int generationNo = generationRepository.countByMemoryMemoryId(memoryId) + 1;
        if (generationNo > freeGenerationLimit) {
            creditService.deduct(userId, creditPolicy.editionRegenerateCost());
        }

        EditionGeneration generation = generationRepository.save(
                EditionGeneration.builder()
                        .memory(memory)
                        .generationNo(generationNo)
                        .storySnapshot(memory.resolveStoryForGeneration())
                        .targetCategoryMain(targetCategoryMain)
                        .targetCategorySub(targetCategorySub)
                        .build());

        List<EditionConcept> concepts = new ArrayList<>();
        for (int order = 1; order <= CONCEPTS_PER_GENERATION; order++) {
            concepts.add(conceptRepository.save(
                    EditionConcept.builder()
                            .generation(generation)
                            .displayOrder(order)
                            .build()));
        }

        eventPublisher.publishEvent(new PipelineEvents.GenerationCreated(generation.getGenerationId()));
        return new Generated(generation, concepts);
    }

    @Transactional
    public EditionGeneration selectEditionName(Long userId, Long generationId, String editionName) {
        EditionGeneration generation = getGeneration(userId, generationId);
        generation.selectEditionName(editionName);
        return generation;
    }

    public EditionGeneration getGeneration(Long userId, Long generationId) {
        EditionGeneration generation = generationRepository.findWithOwner(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.EDITION_NOT_FOUND));
        if (!generation.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.EDITION_NOT_FOUND);
        }
        return generation;
    }

    public Generated getGenerationWithConcepts(Long userId, Long generationId) {
        EditionGeneration generation = getGeneration(userId, generationId);
        return new Generated(
                generation,
                conceptRepository.findAllByGenerationGenerationIdOrderByDisplayOrder(generationId));
    }

    /** 추억 하나의 생성 이력. 콘셉트는 배치마다 조회하지 않고 한 번에 가져와 묶는다. */
    public Page<Generated> getGenerationsByMemory(Long userId, Long memoryId, Pageable pageable) {
        memoryService.getMemory(userId, memoryId);
        Page<EditionGeneration> page =
                generationRepository.findAllByMemoryMemoryIdOrderByGenerationNoDesc(memoryId, pageable);
        Map<Long, List<EditionConcept>> conceptsByGenerationId = conceptRepository
                .findAllByGenerationGenerationIdInOrderByDisplayOrder(
                        page.getContent().stream().map(EditionGeneration::getGenerationId).toList())
                .stream()
                .collect(Collectors.groupingBy(c -> c.getGeneration().getGenerationId()));
        return page.map(generation ->
                new Generated(generation, conceptsByGenerationId.getOrDefault(generation.getGenerationId(), List.of())));
    }

    public record Generated(EditionGeneration generation, List<EditionConcept> concepts) {
    }
}
