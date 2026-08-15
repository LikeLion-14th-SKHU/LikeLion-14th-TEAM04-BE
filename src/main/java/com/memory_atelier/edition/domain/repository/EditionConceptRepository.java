package com.memory_atelier.edition.domain.repository;

import com.memory_atelier.edition.domain.EditionConcept;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EditionConceptRepository extends JpaRepository<EditionConcept, Long> {

    List<EditionConcept> findAllByGenerationGenerationIdOrderByDisplayOrder(Long generationId);

    List<EditionConcept> findAllByGenerationGenerationIdInOrderByDisplayOrder(List<Long> generationIds);

    // 확정 범위는 생성 배치 단위다
    // 새로 확정하기 전에 같은 배치의 이전 확정을 해제해야 한다
    List<EditionConcept> findAllByGenerationGenerationIdAndIsFinalTrue(Long generationId);

    // 소유권 확인은 concept → generation → memory → user를 타고 올라가므로, 지연 로딩이면 매번 3번 더 조회한다
    @Query("select c from EditionConcept c "
            + "join fetch c.generation g join fetch g.memory m join fetch m.user "
            + "where c.conceptId = :conceptId")
    Optional<EditionConcept> findWithOwner(@Param("conceptId") Long conceptId);

    // 아직 잠긴 콘셉트만 조건부로 연다. 조회 후 검사 방식과 달리 동시 요청 중 하나만 반영되므로,
    // 같은 콘셉트를 두 번 열면서 크레딧을 두 번 쓰는 일이 없다(UserRepository.deductCredit과 같은 패턴)
    // 반환값이 0이면 이미 열려 있었다는 뜻이다
    @Modifying(flushAutomatically = true)
    @Query("update EditionConcept c set c.isUnlocked = true where c.conceptId = :conceptId and c.isUnlocked = false")
    int unlockIfLocked(@Param("conceptId") Long conceptId);
}
