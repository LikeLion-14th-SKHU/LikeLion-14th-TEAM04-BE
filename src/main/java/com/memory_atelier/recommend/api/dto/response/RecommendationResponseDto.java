package com.memory_atelier.recommend.api.dto.response;

import com.memory_atelier.recommend.domain.Recommendation;
import com.memory_atelier.recommend.domain.RecommendationItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "MCM 상품 추천 목록")
public record RecommendationResponseDto(
        @Schema(description = "콘셉트 id", example = "1")
        Long conceptId,

        @Schema(description = "추천 상품 목록(2~3개)")
        List<RecommendationItem> recommendations
) {
    public static RecommendationResponseDto from(Recommendation recommendation) {
        return new RecommendationResponseDto(
                recommendation.getConcept().getConceptId(), recommendation.getItems());
    }
}
