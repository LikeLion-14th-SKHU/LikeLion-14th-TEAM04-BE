package com.memory_atelier.recommend.api;

import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.recommend.api.dto.response.RecommendationResponseDto;
import com.memory_atelier.recommend.application.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/edition-concepts/{conceptId}/recommendations")
@Tag(name = "상품 추천(Recommend) API", description = "확정된 콘셉트에 대한 MCM 상품 추천 조회 API")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    @Operation(
            summary = "MCM 상품 추천 조회",
            description = "확정된 콘셉트에 어울리는 MCM 상품 2~3개를 조회합니다. 확정 직후 3D 변환과 같은 파이프라인에서 "
                    + "자동으로 만들어지므로 별도 생성 요청은 없습니다. 아직 준비되지 않았으면 404입니다."
    )
    public ResponseEntity<ApiResponse<RecommendationResponseDto>> getRecommendation(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, RecommendationResponseDto.from(
                recommendationService.getOwnedRecommendation(userId, conceptId)));
    }
}
