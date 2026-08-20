package com.memory_atelier.edition.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 에디션 생성 요청 — 이 추억을 MCM의 어떤 제품군으로 재해석할지 목표 카테고리를 지정한다
public record EditionGenerationRequestDto(
        @Schema(description = "목표 카테고리 대분류. 의류 / 가방 / 악세사리", example = "가방")
        @NotBlank(message = "목표 카테고리 대분류는 필수입니다.")
        @Size(max = 50, message = "목표 카테고리 대분류는 50자를 넘을 수 없습니다.")
        String categoryMain,

        @Schema(
                description = "목표 카테고리 중분류(대분류에 종속). 의류: 니트/가디건/셔츠/자켓/원피스/후드티/블라우스/팬츠, "
                        + "가방: 핸드백/토트백/백팩/클러치/트래블, 악세사리: 벨트/스카프/지갑/키링/헤어밴드",
                example = "토트백")
        @NotBlank(message = "목표 카테고리 중분류는 필수입니다.")
        @Size(max = 50, message = "목표 카테고리 중분류는 50자를 넘을 수 없습니다.")
        String categorySub
) {
}
