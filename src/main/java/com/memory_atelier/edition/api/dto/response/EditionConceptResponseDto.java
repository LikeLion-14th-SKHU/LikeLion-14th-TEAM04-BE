package com.memory_atelier.edition.api.dto.response;

import com.memory_atelier.edition.domain.AppliedElement;
import com.memory_atelier.edition.domain.EditionConcept;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record EditionConceptResponseDto(
        @Schema(description = "콘셉트 id", example = "1")
        Long conceptId,

        @Schema(description = "노출 순서(1~3)", example = "1")
        int displayOrder,

        @Schema(description = "생성 진행 상태", example = "IMAGE_READY", allowableValues = {"PENDING", "IMAGE_READY", "FAILED"})
        String status,

        @Schema(description = "열람 가능 여부", example = "true")
        boolean isUnlocked,

        @Schema(description = "콘셉트 이미지 URL(잠겼거나 생성 전이면 null)")
        String imageUrl,

        @Schema(description = "이 후보의 에디션 카테고리(자동 제안 모드면 후보마다 다를 수 있음, 잠겼으면 null)")
        String category,

        @Schema(description = "후보 카드용 짧은 컨셉명(잠겼으면 null)")
        String conceptName,

        @Schema(description = "재창조 방식 라벨: safe/balanced/bold(잠겼으면 null)")
        String riskProfile,

        @Schema(description = "재창조 방식 자유 서술(잠겼으면 null)")
        String creationMethod,

        @Schema(description = "옷의 어떤 요소를 어디에 왜 적용했는지(잠겼으면 빈 배열)")
        List<AppliedElement> appliedElements,

        @Schema(description = "생성일시")
        Instant createdAt
) {
    // 잠긴 후보는 이미지뿐 아니라 재창조 방식·근거도 가린다
    // 결제 없이 내용을 알 수 있으면 열람 결제가 무의미해진다
    public static EditionConceptResponseDto from(EditionConcept concept) {
        boolean visible = concept.isUnlocked();
        return new EditionConceptResponseDto(
                concept.getConceptId(),
                concept.getDisplayOrder(),
                concept.getStatus().name(),
                concept.isUnlocked(),
                visible ? concept.getImageUrl() : null,
                concept.getCategory(),
                visible ? concept.getConceptName() : null,
                visible ? concept.getRiskProfile() : null,
                visible ? concept.getCreationMethod() : null,
                visible ? concept.getAppliedElements() : List.of(),
                concept.getCreatedAt());
    }
}
