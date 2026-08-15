package com.memory_atelier.certificate.domain;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 최종 확정은 생성 배치(EditionGeneration)당 한 번뿐이다. 존재 여부를 조회해서 막으면 동시 요청이
// 둘 다 통과할 수 있으므로(check-then-act) DB 유니크 제약으로 직접 막는다
@Entity
@Table(
        name = "certificates",
        uniqueConstraints = @UniqueConstraint(columnNames = "generation_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certificate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Long certificateId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false, unique = true)
    private EditionConcept concept;

    // EditionConcept을 타고 올라가면 알 수 있지만, "배치당 보증서 하나"를 유니크 제약으로
    // 걸려면 컬럼으로 직접 들고 있어야 한다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private EditionGeneration generation;

    @Column(nullable = false, unique = true)
    private String editionNumber;

    // 아래 필드는 전부 발급 시점 스냅샷이다. 이후 에디션명을 바꾸거나 추억을 수정해도
    // 이미 발급된 보증서 내용은 따라 바뀌지 않는다
    @Column(nullable = false)
    private String editionName;

    @Column(nullable = false)
    private String category;

    @Column(length = 1000)
    private String certificateText;

    @Column(length = 500)
    private String storySnapshot;

    @Builder
    private Certificate(
            EditionConcept concept,
            EditionGeneration generation,
            String editionNumber,
            String editionName,
            String category,
            String certificateText,
            String storySnapshot) {
        this.concept = concept;
        this.generation = generation;
        this.editionNumber = editionNumber;
        this.editionName = editionName;
        this.category = category;
        this.certificateText = certificateText;
        this.storySnapshot = storySnapshot;
    }

    // 보증서의 주인은 콘셉트의 주인이다
    public boolean isOwnedBy(Long userId) {
        return this.concept.isOwnedBy(userId);
    }
}
