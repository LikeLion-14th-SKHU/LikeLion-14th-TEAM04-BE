package com.memory_atelier.memory.api.dto.response;

import com.memory_atelier.memory.domain.Memory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record MemoryResponseDto(
        @Schema(description = "추억 id", example = "1")
        Long memoryId,

        @Schema(description = "옷/소품 사진 URL", example = "https://memory-atelier.s3.ap-northeast-2.amazonaws.com/abc123.jpg")
        String photoUrl,

        @Schema(description = "카테고리 대분류", example = "가방")
        String categoryMain,

        @Schema(description = "카테고리 중분류", example = "백팩")
        String categorySub,

        @Schema(description = "사용자가 고른 재질", example = "가죽")
        String materialUser,

        @Schema(description = "사용자가 고른 상태 토글", example = "[\"색 바램\"]")
        List<String> condition,

        @Schema(description = "사연 원문", example = "대학 시절 첫 아르바이트로 산 가방이에요.")
        String story,

        @Schema(description = "AI로 다듬은 사연(분석 전에는 null)")
        String storyRefined,

        @Schema(description = "에디션 생성 시 다듬은 사연을 사용할지 여부", example = "false")
        boolean useRefinedStory,

        @Schema(description = "목표 MCM 에디션 카테고리, 미선택이면 null", example = "가방")
        String editionCategory,

        @Schema(description = "AI 분석 결과(분석 전에는 null)")
        AnalysisSummaryDto analysis,

        @Schema(description = "생성일시")
        Instant createdAt
) {
    // Stage 1 분석 결과 중 사용자에게 보여줄 부분. {@code storyInterpretation}(내부용 해석)은 여기에 없다 — 에디션명·큐레이션의 내부 근거로만 쓰고 사용자에게 노출하지 않기로 한 값이다.
    public record AnalysisSummaryDto(
            @Schema(description = "추출된 색상 팔레트", example = "[\"#4A6B8A\", \"#D9CBB3\"]")
            List<String> colorPalette,

            @Schema(description = "패턴", example = "무지")
            String pattern,

            @Schema(description = "사진으로 추정한 재질", example = "가죽")
            String materialEstimate,

            @Schema(description = "최종 채택된 재질(사용자 선택 우선, '선택안함'이면 추정값)", example = "가죽")
            String materialFinal,

            @Schema(description = "상태 단서(사용자 선택 + AI 발견의 합집합)", example = "[\"색 바램\"]")
            List<String> conditionCues,

            @Schema(description = "무드 키워드", example = "[\"빈티지\", \"차분함\"]")
            List<String> vibeKeywords,

            @Schema(description = "감정 키워드", example = "[\"설렘\", \"그리움\"]")
            List<String> emotionKeywords
    ) {
    }

    public static MemoryResponseDto from(Memory memory) {
        return new MemoryResponseDto(
                memory.getMemoryId(),
                memory.getPhotoUrl(),
                memory.getCategoryMain(),
                memory.getCategorySub(),
                memory.getMaterialUser(),
                memory.getConditionTags(),
                memory.getStory(),
                memory.getStoryRefined(),
                memory.isUseRefinedStory(),
                memory.getEditionCategory(),
                toAnalysis(memory),
                memory.getCreatedAt());
    }

    // 분석 전에는 통째로 null을 내려 프론트가 "아직 분석 안 됨"을 한 번에 판단하게 한다.
    private static AnalysisSummaryDto toAnalysis(Memory memory) {
        boolean analyzed = memory.getMaterialFinal() != null
                || memory.getPattern() != null
                || !memory.getColorPalette().isEmpty()
                || !memory.getVibeKeywords().isEmpty();
        if (!analyzed) {
            return null;
        }
        return new AnalysisSummaryDto(
                memory.getColorPalette(),
                memory.getPattern(),
                memory.getMaterialEstimate(),
                memory.getMaterialFinal(),
                memory.getConditionCues(),
                memory.getVibeKeywords(),
                memory.getEmotionKeywords());
    }
}
