package com.memory_atelier.memory.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

// 추억 등록 요청
// multipart/form-data로 사진 파일과 함께 받는다
public record MemorySaveRequestDto(
        @Schema(description = "옷/소품 사진 파일")
        MultipartFile photo,

        @Schema(description = "카테고리 대분류. 의류 / 가방 / 악세사리", example = "가방")
        @Size(max = 50, message = "카테고리 대분류는 50자를 넘을 수 없습니다.")
        String categoryMain,

        @Schema(
                description = "카테고리 중분류(대분류에 종속). 의류: 니트/가디건/셔츠/자켓/원피스/후드티/블라우스/팬츠, "
                        + "가방: 핸드백/토트백/백팩/클러치/트래블, 악세사리: 벨트/스카프/지갑/키링/헤어밴드",
                example = "백팩")
        @Size(max = 50, message = "카테고리 중분류는 50자를 넘을 수 없습니다.")
        String categorySub,

        @Schema(
                description = "재질 토글 선택값. 데님 / 가죽 / 니트 / 울 / 면 / 린넨 / 벨벳 / 레이스 / 선택안함. "
                        + "'선택안함'이면 AI가 사진으로 재질을 추정합니다.",
                example = "가죽")
        @Size(max = 50, message = "재질은 50자를 넘을 수 없습니다.")
        String materialUser,

        @Schema(description = "상태 토글 선택값(복수·생략 가능). 해짐 / 색 바램 / 얼룩 / 늘어남 / 새것 같음", example = "[\"색 바램\"]")
        @Size(max = 10, message = "상태는 10개를 넘을 수 없습니다.")
        List<@Size(max = 50, message = "상태 항목은 50자를 넘을 수 없습니다.") String> condition,

        @Schema(description = "한 줄 사연", example = "대학 시절 첫 아르바이트로 산 가방이에요.")
        @NotBlank(message = "사연은 필수입니다.")
        @Size(max = 500, message = "사연은 500자를 넘을 수 없습니다.")
        String story,

        @Schema(description = "목표 MCM 에디션 카테고리. 생략하면 AI가 후보마다 카테고리를 제안합니다.", example = "가방")
        @Size(max = 50, message = "에디션 카테고리는 50자를 넘을 수 없습니다.")
        String editionCategory
) {
}
