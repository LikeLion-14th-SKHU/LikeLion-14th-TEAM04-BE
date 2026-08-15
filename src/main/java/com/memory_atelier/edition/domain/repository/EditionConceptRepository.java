package com.memory_atelier.edition.domain.repository;

import com.memory_atelier.edition.domain.EditionConcept;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EditionConceptRepository extends JpaRepository<EditionConcept, Long> {

    List<EditionConcept> findAllByGenerationGenerationIdOrderByDisplayOrder(Long generationId);

    List<EditionConcept> findAllByGenerationGenerationIdInOrderByDisplayOrder(List<Long> generationIds);
}
