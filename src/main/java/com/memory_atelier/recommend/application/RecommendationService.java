package com.memory_atelier.recommend.application;

import com.memory_atelier.edition.application.PipelineEvents;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.recommend.domain.Recommendation;
import com.memory_atelier.recommend.domain.RecommendationItem;
import com.memory_atelier.recommend.domain.repository.RecommendationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final EditionConceptRepository conceptRepository;

    // 3D 변환과 같은 job에서 온 큐레이션 결과를 스냅샷 저장한다
    // 사용자 액션이 아니라 파이프라인이 비동기로 호출하므로, 이미 저장돼 있으면(이벤트가 중복으로 와도) 조용히 무시한다
    @Transactional
    public void saveFromCuration(Long conceptId, List<PipelineEvents.CurationReceived.RecommendationPayload> payload) {
        if (recommendationRepository.existsByConceptConceptId(conceptId)) {
            return;
        }
        EditionConcept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONCEPT_NOT_FOUND));
        List<RecommendationItem> items = payload.stream()
                .map(p -> new RecommendationItem(p.productId(), p.nameKr(), p.reason(), p.tagline(), p.imageUrl()))
                .toList();
        recommendationRepository.save(Recommendation.builder().concept(concept).items(items).build());
    }

    // 추천 목록 조회. 소유하지 않았거나 아직 큐레이션이 안 끝났으면 구분 없이 404로 거절한다(소유권 은닉 정책)
    public Recommendation getOwnedRecommendation(Long userId, Long conceptId) {
        Recommendation recommendation = recommendationRepository.findByConceptId(conceptId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        if (!recommendation.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND);
        }
        return recommendation;
    }
}
