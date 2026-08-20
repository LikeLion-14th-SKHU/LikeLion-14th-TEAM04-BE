package com.memory_atelier.certificate.api;

import com.memory_atelier.certificate.api.dto.request.CollectionNameRequestDto;
import com.memory_atelier.certificate.api.dto.request.CollectionThemeRequestDto;
import com.memory_atelier.certificate.api.dto.response.CollectionCardResponseDto;
import com.memory_atelier.certificate.api.dto.response.CollectionSettingsResponseDto;
import com.memory_atelier.certificate.application.CertificateService;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.PageResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.user.application.UserService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/me/collection")
@Tag(name = "컬렉션(Collection) API", description = "내 컬렉션(전시 카드) 조회 및 이름·테마 설정 API")
public class CollectionController {

    private final CertificateService certificateService;
    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "내 컬렉션 목록 조회",
            description = "최종 확정되어 보증서가 발급된 전시 카드 목록입니다. 확정 시 별도 저장 액션 없이 자동으로 등록됩니다. "
                    + "아직 확정하지 않은 후보 콘셉트는 GET /edition-generations/{generationId}에서 확인합니다."
    )
    public ResponseEntity<ApiResponse<PageResponse<CollectionCardResponseDto>>> getMyCollection(
            @AuthenticationPrincipal Long userId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, PageResponse.of(
                certificateService.getMyCertificates(userId, pageable), CollectionCardResponseDto::from));
    }

    @GetMapping("/{conceptId}")
    @Operation(summary = "전시 카드 상세 조회", description = "확정된 전시 카드만 조회됩니다. 소유자만 볼 수 있습니다.")
    public ResponseEntity<ApiResponse<CollectionCardResponseDto>> getCollectionItem(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, CollectionCardResponseDto.from(
                certificateService.getOwnedCertificate(userId, conceptId)));
    }

    @PatchMapping("/name")
    @Operation(summary = "컬렉션 이름 변경", description = "내 컬렉션 전시 페이지에 표시할 이름을 변경합니다.")
    public ResponseEntity<ApiResponse<CollectionSettingsResponseDto>> updateCollectionName(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CollectionNameRequestDto requestDto) {
        return ApiResponse.success(SuccessCode.OK, CollectionSettingsResponseDto.from(
                userService.updateCollectionName(userId, requestDto.collectionName())));
    }

    @PatchMapping("/theme")
    @Operation(
            summary = "컬렉션 테마 변경",
            description = "내 컬렉션 전시 페이지의 배경/카드 스타일 테마를 변경합니다. "
                    + "WHITE / IVORY / BUTTER / OLIVE_CREAM / DUSTY_ROSE / LIGHT_MOCHA 중 하나를 고릅니다."
    )
    public ResponseEntity<ApiResponse<CollectionSettingsResponseDto>> updateCollectionTheme(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CollectionThemeRequestDto requestDto) {
        return ApiResponse.success(SuccessCode.OK, CollectionSettingsResponseDto.from(
                userService.updateCollectionTheme(userId, requestDto.theme())));
    }
}
