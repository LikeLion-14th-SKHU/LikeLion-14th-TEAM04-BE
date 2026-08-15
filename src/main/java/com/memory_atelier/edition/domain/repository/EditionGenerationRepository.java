package com.memory_atelier.edition.domain.repository;

import com.memory_atelier.edition.domain.EditionGeneration;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EditionGenerationRepository extends JpaRepository<EditionGeneration, Long> {

    int countByMemoryMemoryId(Long memoryId);

    Page<EditionGeneration> findAllByMemoryMemoryIdOrderByGenerationNoDesc(Long memoryId, Pageable pageable);

    // 소유자까지 한 번에 로딩해 컨트롤러/서비스에서 owner 체크 시 추가 쿼리가 안 나가게 한다.
    @Query("select eg from EditionGeneration eg "
            + "join fetch eg.memory m join fetch m.user "
            + "where eg.generationId = :generationId")
    Optional<EditionGeneration> findWithOwner(@Param("generationId") Long generationId);
}
