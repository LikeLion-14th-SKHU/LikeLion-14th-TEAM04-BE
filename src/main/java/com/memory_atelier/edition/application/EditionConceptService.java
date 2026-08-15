package com.memory_atelier.edition.application;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.user.application.CreditPolicy;
import com.memory_atelier.user.application.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EditionConceptService {

    private final EditionConceptRepository conceptRepository;
    private final CreditService creditService;
    private final CreditPolicy creditPolicy;

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
}
