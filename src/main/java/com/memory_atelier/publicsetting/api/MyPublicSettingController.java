package com.memory_atelier.publicsetting.api;

import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.publicsetting.api.dto.request.ToggleVisibilityRequestDto;
import com.memory_atelier.publicsetting.api.dto.response.MyPublicSettingsResponseDto;
import com.memory_atelier.publicsetting.api.dto.response.PublicSettingResponseDto;
import com.memory_atelier.publicsetting.application.PublicSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 내 공개 설정. 전부 인증이 필요하고, 남의 설정은 건드릴 수 없다
// 남의 것을 구경하는 읽기 API는 Community 도메인 몫이다(/community)
@RestController
@RequiredArgsConstructor
@RequestMapping("/me/public-settings")
@Tag(name = "내 공개 설정(MyPublicSetting) API", description = "컬렉션·카드 공개 설정 및 공유 토큰 API")
public class MyPublicSettingController {

    private final PublicSettingService publicSettingService;

    @GetMapping
    @Operation(
            summary = "내 공개 설정 조회",
            description = "공개 토글의 현재 상태와 공유 링크 토큰을 함께 내려줍니다. cards에는 카드 단위로 따로 지정한 "
                    + "설정만 담기고, 목록에 없는 카드는 collection 설정을 따릅니다. 카드 설정이 있으면 그 값이 컬렉션 설정을 덮습니다."
    )
    public ResponseEntity<ApiResponse<MyPublicSettingsResponseDto>> getMySettings(
            @AuthenticationPrincipal Long userId) {
        PublicSettingService.MySettings settings = publicSettingService.getMySettings(userId);
        return ApiResponse.success(SuccessCode.GET_SUCCESS, MyPublicSettingsResponseDto.of(
                settings.collection(), settings.cards()));
    }

    @PatchMapping("/collection")
    @Operation(summary = "컬렉션 전체 공개 설정")
    public ResponseEntity<ApiResponse<PublicSettingResponseDto>> toggleCollection(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ToggleVisibilityRequestDto request) {
        return ApiResponse.success(SuccessCode.OK, PublicSettingResponseDto.from(
                publicSettingService.toggleCollection(userId, request.isPublic())));
    }

    @PatchMapping("/cards/{conceptId}")
    @Operation(
            summary = "카드 단위 공개 설정",
            description = "카드 단위 설정은 컬렉션 전체 공개보다 우선합니다. 전체 공개 상태여도 여기서 비공개로 두면 "
                    + "노출되지 않고, 반대로 컬렉션이 비공개여도 여기서 공개한 카드는 피드에 올라갑니다."
    )
    public ResponseEntity<ApiResponse<PublicSettingResponseDto>> toggleCard(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId,
            @Valid @RequestBody ToggleVisibilityRequestDto request) {
        return ApiResponse.success(SuccessCode.OK, PublicSettingResponseDto.from(
                publicSettingService.toggleCard(userId, conceptId, request.isPublic())));
    }
}
