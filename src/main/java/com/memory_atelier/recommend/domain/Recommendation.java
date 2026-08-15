package com.memory_atelier.recommend.domain;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 확정된 콘셉트에 대한 MCM 상품 추천. 3D 변환과 같은 job에서 나오는 큐레이션 결과를 사용자 액션
// 없이 자동으로 스냅샷 저장한다(EditionPipelineRunner → PipelineEvents.CurationReceived)
@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false, unique = true)
    private EditionConcept concept;

    @Convert(converter = RecommendationItemListConverter.class)
    @Column(length = 2000)
    private List<RecommendationItem> items;

    @Builder
    private Recommendation(EditionConcept concept, List<RecommendationItem> items) {
        this.concept = concept;
        this.items = (items != null) ? items : List.of();
    }

    // 추천의 주인은 콘셉트의 주인이다
    public boolean isOwnedBy(Long userId) {
        return this.concept.isOwnedBy(userId);
    }
}
