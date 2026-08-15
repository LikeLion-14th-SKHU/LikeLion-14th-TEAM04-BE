package com.memory_atelier.edition.api.dto.response;

import com.memory_atelier.edition.application.EditionGenerationService.Generated;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record EditionGenerationResponseDto(
        @Schema(description = "에디션 생성 배치 id", example = "1")
        Long generationId,

        @Schema(description = "재생성 회차(1부터 시작)", example = "1")
        int generationNo,

        @Schema(description = "선택된 에디션명(생성 직후에는 null)")
        String editionName,

        @Schema(description = "에디션명 후보. 이 중 다른 값으로 바꿀 수 있음")
        List<String> editionNameCandidates,

        @Schema(description = "이 배치에서 생성된 콘셉트 3장")
        List<EditionConceptResponseDto> concepts,

        @Schema(description = "생성일시")
        Instant createdAt
) {
    public static EditionGenerationResponseDto of(EditionGeneration generation, List<EditionConcept> concepts) {
        return new EditionGenerationResponseDto(
                generation.getGenerationId(),
                generation.getGenerationNo(),
                generation.getEditionName(),
                generation.getEditionNameCandidates(),
                concepts.stream().map(EditionConceptResponseDto::from).toList(),
                generation.getCreatedAt());
    }

    public static EditionGenerationResponseDto from(Generated generated) {
        return of(generated.generation(), generated.concepts());
    }
}
