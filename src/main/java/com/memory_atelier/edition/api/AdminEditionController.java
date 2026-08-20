package com.memory_atelier.edition.api;

import com.memory_atelier.edition.api.dto.response.EditionGenerationResponseDto;
import com.memory_atelier.edition.application.EditionGenerationService;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.PageResponse;
import com.memory_atelier.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /memories/{memoryId}/edition-generations(일반 사용자용)와 목록 형태를 그대로 맞췄다.
// 다른 점은 소유자 체크가 없고(어느 유저의 추억이든 조회 가능), 잠긴 콘셉트도 마스킹 없이
// 3장 전체를 그대로 내려준다는 것뿐이다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/memories/{memoryId}/edition-generations")
@Tag(name = "에디션 생성 관리(Admin) API", description = "잠금 여부와 무관하게 콘셉트 3장 전체를 조회합니다. ADMIN 권한이 필요합니다.")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEditionController {

    private final EditionGenerationService editionGenerationService;

    @GetMapping
    @Operation(
            summary = "추억 하나의 에디션 생성 이력 조회 (관리자, 마스킹 없음)",
            description = "일반 API(GET /memories/{memoryId}/edition-generations)와 목록 형태는 동일하지만, "
                    + "소유자가 아니어도 조회할 수 있고 잠긴 콘셉트도 이미지·재창조 방식 등 전체 내용을 그대로 내려줍니다. "
                    + "크레딧 결제 우회 확인용이므로 프론트 일반 화면에는 쓰지 마세요."
    )
    public ResponseEntity<ApiResponse<PageResponse<EditionGenerationResponseDto>>> getGenerationsByMemory(
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, PageResponse.of(
                editionGenerationService.getGenerationsByMemoryForAdmin(memoryId, pageable),
                EditionGenerationResponseDto::ofUnmasked));
    }
}
