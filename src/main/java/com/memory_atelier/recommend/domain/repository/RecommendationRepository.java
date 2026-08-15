package com.memory_atelier.recommend.domain.repository;

import com.memory_atelier.recommend.domain.Recommendation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    boolean existsByConceptConceptId(Long conceptId);

    // 소유권 확인은 recommendation → concept → generation → memory → user를 타고 올라가므로 함께 가져온다
    @Query("""
            select r from Recommendation r
              join fetch r.concept concept
              join fetch concept.generation g
              join fetch g.memory m
              join fetch m.user
            where concept.conceptId = :conceptId
            """)
    Optional<Recommendation> findByConceptId(@Param("conceptId") Long conceptId);
}
