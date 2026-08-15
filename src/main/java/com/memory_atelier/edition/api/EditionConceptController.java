package com.memory_atelier.edition.api;

import com.memory_atelier.edition.api.dto.response.EditionConceptResponseDto;
import com.memory_atelier.edition.application.EditionConceptService;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "에디션 콘셉트(Edition Concept) API", description = "잠긴 콘셉트 열람 잠금 해제 API")
public class EditionConceptController {

    private final EditionConceptService editionConceptService;

    @PostMapping("/edition-concepts/{conceptId}/unlock")
    @Operation(
            summary = "콘셉트 열람 잠금 해제",
            description = "크레딧을 차감하고 잠긴 콘셉트를 열람 가능하게 합니다. 잔액이 모자라면 크레딧 부족 에러로 거절되며, "
                    + "잔액은 GET /me에서 확인합니다. 이미 열람 가능하거나 아직 이미지가 준비되지 않은 콘셉트는 거절됩니다."
    )
    public ResponseEntity<ApiResponse<EditionConceptResponseDto>> unlock(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        return ApiResponse.success(SuccessCode.OK, EditionConceptResponseDto.from(
                editionConceptService.unlock(userId, conceptId)));
    }
}
