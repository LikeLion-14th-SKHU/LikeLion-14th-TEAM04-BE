package com.memory_atelier.edition.api;

import com.memory_atelier.edition.api.dto.request.EditionNameRequestDto;
import com.memory_atelier.edition.api.dto.response.EditionGenerationResponseDto;
import com.memory_atelier.edition.application.EditionGenerationService;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.PageResponse;
import com.memory_atelier.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "에디션 생성(Edition Generation) API", description = "AI 파이프라인으로 콘셉트 3장을 생성하고 폴링·에디션명 선택하는 API")
public class EditionGenerationController {

    private final EditionGenerationService editionGenerationService;

    @PostMapping("/memories/{memoryId}/edition-generations")
    @Operation(
            summary = "에디션(콘셉트 3장) 생성 요청",
            description = "202 Accepted — 생성을 접수만 하고 즉시 응답합니다. AI 파이프라인은 뒤에서 비동기로 돌기 때문에 "
                    + "응답 시점의 콘셉트 3장은 모두 status=PENDING이고 imageUrl은 null입니다. "
                    + "응답의 generationId로 GET /edition-generations/{generationId}를 폴링해서 status가 IMAGE_READY가 되면 후보를 보여주세요(2초 간격 권장). "
                    + "무료 생성 횟수를 넘긴 회차부터는 크레딧이 차감되며, 잔액이 모자라면 거절됩니다. AI 분석이 안 된 추억은 생성할 수 없습니다."
    )
    public ResponseEntity<ApiResponse<EditionGenerationResponseDto>> generate(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId) {
        EditionGenerationService.Generated generated = editionGenerationService.generate(userId, memoryId);
        return ApiResponse.success(SuccessCode.ACCEPTED, EditionGenerationResponseDto.from(generated));
    }

    @GetMapping("/memories/{memoryId}/edition-generations")
    @Operation(summary = "추억 하나의 에디션 생성 이력 조회")
    public ResponseEntity<ApiResponse<PageResponse<EditionGenerationResponseDto>>> getGenerationsByMemory(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, PageResponse.of(
                editionGenerationService.getGenerationsByMemory(userId, memoryId, pageable),
                EditionGenerationResponseDto::from));
    }

    @GetMapping("/edition-generations/{generationId}")
    @Operation(
            summary = "에디션 생성 배치 상세 조회 (진행 상태 폴링용)",
            description = "생성 요청 후 이 API를 폴링해 콘셉트의 status가 PENDING → IMAGE_READY로 바뀌는지 확인합니다. "
                    + "FAILED면 재생성을 안내하세요. 잠긴(isUnlocked=false) 콘셉트는 이미지·컨셉명 등 상세 정보가 가려집니다."
    )
    public ResponseEntity<ApiResponse<EditionGenerationResponseDto>> getGeneration(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "에디션 생성 배치 id", example = "1") @PathVariable Long generationId) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, EditionGenerationResponseDto.from(
                editionGenerationService.getGenerationWithConcepts(userId, generationId)));
    }

    @PatchMapping("/edition-generations/{generationId}/edition-name")
    @Operation(
            summary = "에디션명 선택",
            description = "AI가 제안한 editionNameCandidates 중 하나를 그대로 쓰거나, 마음에 안 들면 직접 지은 이름으로 바꿀 수 있습니다(50자 이내)."
    )
    public ResponseEntity<ApiResponse<EditionGenerationResponseDto>> selectEditionName(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "에디션 생성 배치 id", example = "1") @PathVariable Long generationId,
            @Valid @RequestBody EditionNameRequestDto requestDto) {
        editionGenerationService.selectEditionName(userId, generationId, requestDto.editionName());
        return ApiResponse.success(SuccessCode.OK, EditionGenerationResponseDto.from(
                editionGenerationService.getGenerationWithConcepts(userId, generationId)));
    }
}
