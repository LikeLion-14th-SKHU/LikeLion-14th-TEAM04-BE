package com.memory_atelier.edition.domain;

import com.memory_atelier.global.entity.BaseTimeEntity;
import com.memory_atelier.global.entity.StringListConverter;
import com.memory_atelier.memory.domain.Memory;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "edition_generations",
        // 같은 추억에서 같은 회차 번호가 두 번 생기지 않도록 DB에서도 막는다.
        uniqueConstraints = @UniqueConstraint(columnNames = {"memory_id", "generation_no"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EditionGeneration extends BaseTimeEntity {

    public static final int CERTIFICATE_TEXT_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "generation_id")
    private Long generationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memory_id", nullable = false)
    private Memory memory;

    @Column(name = "generation_no", nullable = false)
    private int generationNo;

    /** 선택된 에디션명. 생성 직후·확정 전에는 비어 있다. */
    private String editionName;

    /** Stage 1의 {@code edition_name_candidates}. */
    @Convert(converter = StringListConverter.class)
    @Column(length = 500)
    private List<String> editionNameCandidates = List.of();

    /** Stage 1의 {@code certificate_text} — 디지털 보증서 문구. */
    @Column(length = CERTIFICATE_TEXT_MAX_LENGTH)
    private String certificateText;

    @Column(nullable = false, length = 500)
    private String storySnapshot;

    /** 이 추억을 재해석할 목표 MCM 제품군 대분류. 의류 / 가방 / 악세사리 */
    @Column(nullable = false, length = 50)
    private String targetCategoryMain;

    /** 목표 제품군 중분류(대분류에 종속). AI에게는 이 값이 target_category로 전달된다. */
    @Column(nullable = false, length = 50)
    private String targetCategorySub;

    /** FastAPI job id. 폴링에 쓰인다. job 접수 자체가 실패하면 비어 있을 수 있다. */
    private String jobId;

    @Builder
    private EditionGeneration(
            Memory memory, int generationNo, String storySnapshot, String targetCategoryMain, String targetCategorySub) {
        this.memory = memory;
        this.generationNo = generationNo;
        this.storySnapshot = storySnapshot;
        this.targetCategoryMain = targetCategoryMain;
        this.targetCategorySub = targetCategorySub;
    }

    public boolean isOwnedBy(Long userId) {
        return this.memory.isOwnedBy(userId);
    }

    public void assignJobId(String jobId) {
        this.jobId = jobId;
    }

    /** FastAPI awaiting_selection 응답의 서사 산출물 반영. 에디션명은 후보 중 첫 번째를 기본 선택으로 둔다. */
    public void applyNarrative(List<String> editionNameCandidates, String certificateText) {
        this.editionNameCandidates = (editionNameCandidates != null) ? editionNameCandidates : List.of();
        this.certificateText = truncateCertificateText(certificateText);
        if (!this.editionNameCandidates.isEmpty()) {
            this.editionName = this.editionNameCandidates.getFirst();
        }
    }

    /** AI 후보 중 하나를 그대로 써도 되고, 마음에 안 들면 직접 지은 이름으로 바꿔도 된다. */
    public void selectEditionName(String editionName) {
        this.editionName = editionName;
    }

    private static String truncateCertificateText(String text) {
        if (text == null || text.length() <= CERTIFICATE_TEXT_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, CERTIFICATE_TEXT_MAX_LENGTH);
    }
}
