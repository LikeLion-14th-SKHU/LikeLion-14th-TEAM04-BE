package com.memory_atelier.edition.application;

import com.memory_atelier.edition.application.PipelineEvents.ConceptFinalized;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.user.application.CreditPolicy;
import com.memory_atelier.user.application.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EditionConceptService {

    private final EditionConceptRepository conceptRepository;
    private final CreditService creditService;
    private final CreditPolicy creditPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public EditionConcept getOwnedConcept(Long userId, Long conceptId) {
        EditionConcept concept = conceptRepository.findWithOwner(conceptId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONCEPT_NOT_FOUND));
        if (!concept.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.CONCEPT_NOT_FOUND);
        }
        return concept;
    }

    // 크레딧을 써서 잠긴 콘셉트를 연다
    // 앞의 두 검사(이미 열림 / 이미지 미준비)는 흔한 경우를 빨리 걸러 크레딧을 아예 쓰지 않기 위한 것일 뿐, 그것만으로 동시 요청을 막지는 못한다
    // 같은 콘셉트에 두 요청이 동시에 오면 둘 다 이 검사를 통과하므로, 실제 반영은 조건부 UPDATE({@code unlockIfLocked})가 최종 관문이다.\
    // 거기서 막힌 쪽은 예외로 트랜잭션이 롤백되며 방금 차감된 크레딧도 함께 되돌아간다
    // 차감을 조건부 UPDATE 앞에 두는 이유도 같다
    // 뒤에 두면 그 UPDATE가 막혔을 때 이미 쓴 크레딧을 되돌리는 보상 처리가 따로 필요해진다
    @Transactional
    public EditionConcept unlock(Long userId, Long conceptId) {
        EditionConcept concept = getOwnedConcept(userId, conceptId);
        if (concept.isUnlocked()) {
            throw new CustomException(ErrorCode.CONCEPT_ALREADY_UNLOCKED);
        }
        if (!concept.getStatus().hasImage()) {
            throw new CustomException(ErrorCode.CONCEPT_NOT_READY);
        }
        creditService.deduct(userId, creditPolicy.editionUnlockCost());
        if (conceptRepository.unlockIfLocked(conceptId) == 0) {
            throw new CustomException(ErrorCode.CONCEPT_ALREADY_UNLOCKED);
        }
        concept.unlock();
        return concept;
    }

    // 콘셉트를 최종 확정한다
    // 보증서 발급 자체는 이 메서드가 하지 않는다
    // edition이 보증서를 알게 되면 certificate와 서로를 참조하게 되므로, 그 지점은 CertificateIssueFacade가 잇는다
    // 확정 범위는 생성 배치 단위다
    // 이전 확정만 해제하면 보증서가 붙어 있는데 isFinal=false인 콘셉트가 남으므로, 같은 배치 안에서만 해제한다
    // "배치당 한 번"을 실제로 강제하는 건 보증서 발급 쪽의 유니크 제약이다
    // 같은 배치의 두 후보를 동시에 확정하면 여기는 둘 다 통과하고, 진 쪽은 발급에서 막혀 이 확정 표시까지 함께 롤백된다
    @Transactional
    public EditionConcept markFinal(Long userId, Long conceptId) {
        EditionConcept concept = getOwnedConcept(userId, conceptId);
        if (!concept.isUnlocked()) {
            throw new CustomException(ErrorCode.EDITION_CONCEPT_LOCKED);
        }
        if (!concept.getStatus().hasImage() || concept.getCategory() == null) {
            throw new CustomException(ErrorCode.CONCEPT_NOT_READY);
        }
        EditionGeneration generation = concept.getGeneration();
        if (generation.getEditionName() == null) {
            throw new CustomException(ErrorCode.CONCEPT_NOT_READY, "에디션명이 아직 생성되지 않았습니다.");
        }

        conceptRepository.findAllByGenerationGenerationIdAndIsFinalTrue(generation.getGenerationId())
                .forEach(EditionConcept::cancelFinal);
        concept.markFinal();

        // Stage 4(3D 변환)는 확정된 한 장에만 돌린다. 30~60초 걸리므로 이 트랜잭션에서 기다리지
        // 않고, 커밋 후 비동기로 넘겨 모델이 준비되는 대로 채운다. 그동안 카드는 2D 이미지로 보인다
        eventPublisher.publishEvent(new ConceptFinalized(concept.getConceptId()));
        return concept;
    }
}
