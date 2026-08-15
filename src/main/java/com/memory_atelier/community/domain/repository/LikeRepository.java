package com.memory_atelier.community.domain.repository;

import com.memory_atelier.community.domain.Like;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByUserUserIdAndConceptConceptId(Long userId, Long conceptId);

    Optional<Like> findByUserUserIdAndConceptConceptId(Long userId, Long conceptId);

    long countByConceptConceptId(Long conceptId);

    // 피드·공유 카드 목록에서 카드마다 좋아요 수를 따로 세지 않도록 한 번에 집계한다
    @Query("""
            select l.concept.conceptId as conceptId, count(l) as likeCount
            from Like l
            where l.concept.conceptId in :conceptIds
            group by l.concept.conceptId
            """)
    List<ConceptLikeCount> countByConceptIdIn(@Param("conceptIds") Collection<Long> conceptIds);

    interface ConceptLikeCount {
        Long getConceptId();

        long getLikeCount();
    }
}
