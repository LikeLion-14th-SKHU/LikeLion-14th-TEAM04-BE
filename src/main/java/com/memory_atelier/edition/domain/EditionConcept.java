package com.memory_atelier.edition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.memory_atelier.global.entity.BaseTimeEntity;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "edition_concepts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"generation_id", "display_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EditionConcept extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concept_id")
    private Long conceptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private EditionGeneration generation;

    /** 노출 순서(1~3). safe/balanced/bold 인덱스와 대응한다. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // ── Stage 2 산출물(design_spec) ──────────────────────────────────────

    private String category;

    @Column(length = 500)
    private String categoryReason;

    private String conceptName;

    /** safe / balanced / bold. */
    private String riskProfile;

    private Integer interventionLevel;

    private String baseProduct;

    @Column(length = 500)
    private String creationMethod;

    @Convert(converter = AppliedElementListConverter.class)
    @Column(length = 2000)
    private List<AppliedElement> appliedElements = List.of();

    // ── Stage 3 산출물 ──────────────────────────────────────────────────

    private String imageUrl;

    /** 검증 게이트가 매긴 품질 점수(0~100). 이미지가 나오기 전에는 null. */
    private Integer qualityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConceptStatus status;

    @Column(nullable = false)
    private boolean isUnlocked;

    @Builder
    private EditionConcept(EditionGeneration generation, int displayOrder) {
        this.generation = generation;
        this.displayOrder = displayOrder;
        this.isUnlocked = false;
        this.status = ConceptStatus.PENDING;
    }

    public boolean isOwnedBy(Long userId) {
        return this.generation.isOwnedBy(userId);
    }

    /** Stage 2·3 결과 반영(검증 게이트 통과분만 여기까지 온다). */
    public void applyConceptImage(
            String category,
            String categoryReason,
            String conceptName,
            String riskProfile,
            Integer interventionLevel,
            String baseProduct,
            String creationMethod,
            List<AppliedElement> appliedElements,
            String imageUrl,
            int qualityScore) {
        this.category = category;
        this.categoryReason = categoryReason;
        this.conceptName = conceptName;
        this.riskProfile = riskProfile;
        this.interventionLevel = interventionLevel;
        this.baseProduct = baseProduct;
        this.creationMethod = creationMethod;
        this.appliedElements = (appliedElements != null) ? appliedElements : List.of();
        this.imageUrl = imageUrl;
        this.qualityScore = qualityScore;
        this.status = ConceptStatus.IMAGE_READY;
    }

    public void markFailed() {
        this.status = ConceptStatus.FAILED;
    }

    public void unlock() {
        this.isUnlocked = true;
    }
}
