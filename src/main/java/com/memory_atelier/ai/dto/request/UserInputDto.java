package com.memory_atelier.ai.dto.request;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.List;

// FastAPI {@code UserInput} 계약(ai_pipeline/schemas/user_input.py)과 1:1 대응
// 값 검증(허용 카테고리·재질 등)은 이 인프라 계층이 아니라 이 DTO를 조립하는 도메인이 책임진다
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserInputDto(
        // {main: "상의"|"하의"|"원피스"|"아우터", sub: 대분류별 중분류(자유 텍스트 직접입력 포함)}
        ClothingCategoryDto category,

        // "데님"|"가죽"|"니트"|"울"|"면"|"린넨"|"벨벳"|"레이스"|"선택안함"
        String material,

        // "해짐"|"색 바램"|"얼룩"|"늘어남"|"새것 같음" 중 복수 선택, 없으면 빈 리스트
        List<String> condition,

        // 사연 원문, 1~500자
        String story
) {
    public record ClothingCategoryDto(
            String main,
            String sub
    ) {
    }
}
