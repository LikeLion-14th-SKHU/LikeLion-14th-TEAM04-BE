package com.memory_atelier.certificate.api.dto.response;

import com.memory_atelier.memory.domain.Memory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 전시 카드 뒷면에 함께 보여줄 원본 옷(추억) 정보 — AI가 재해석한 콘셉트와 대비해서 보여주기 위함
@Schema(description = "원본 옷 정보")
public record OriginalItemDto(
        @Schema(description = "원본 사진 URL", example = "https://cdn.example.com/memories/1/photo.png")
        String photoUrl,

        @Schema(description = "카테고리 대분류", example = "상의")
        String categoryMain,

        @Schema(description = "카테고리 중분류", example = "니트")
        String categorySub,

        @Schema(description = "재질(사용자 선택 우선, 미선택이면 AI 추정값)", example = "울")
        String material,

        @Schema(description = "상태 태그(사용자 선택 + AI 단서 병합)", example = "[\"해짐\", \"색 바램\"]")
        List<String> condition
) {
    public static OriginalItemDto from(Memory memory) {
        return new OriginalItemDto(
                memory.getPhotoUrl(),
                memory.getCategoryMain(),
                memory.getCategorySub(),
                memory.getMaterialFinal(),
                memory.getConditionCues());
    }
}
