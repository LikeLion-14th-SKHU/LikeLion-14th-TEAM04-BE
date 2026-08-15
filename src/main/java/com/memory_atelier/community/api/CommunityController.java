package com.memory_atelier.community.api;

import com.memory_atelier.community.api.dto.response.EditionCardDetailResponseDto;
import com.memory_atelier.community.api.dto.response.EditionFeedItemResponseDto;
import com.memory_atelier.community.api.dto.response.PublicCollectionResponseDto;
import com.memory_atelier.community.api.dto.response.SharedCardResponseDto;
import com.memory_atelier.community.api.dto.response.SharedViewResponseDto;
import com.memory_atelier.community.application.CommunityFeedService;
import com.memory_atelier.community.application.SharedViewService;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.PageResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.publicsetting.application.PublicSettingService;
import com.memory_atelier.publicsetting.domain.PublicSetting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 커뮤니티 읽기 전용 API. 전부 인증 없이 열린다
// 내 공개 설정을 바꾸는 쓰기는 MyPublicSettingController(/me/public-settings)에 있다
@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
@SecurityRequirements
@Tag(name = "커뮤니티(Community) API", description = "공개 커뮤니티 조회 API (인증 불필요)")
public class CommunityController {

    private final CommunityFeedService communityFeedService;
    private final PublicSettingService publicSettingService;
    private final SharedViewService sharedViewService;

    @GetMapping("/editions")
    @Operation(
            summary = "공개 에디션 피드",
            description = "공개된 전시 카드를 정렬해 보여줍니다. sort=POPULAR(좋아요 많은 순, 동률이면 최신)/"
                    + "LATEST(기본값). keyword는 에디션명과 닉네임을 함께 검색합니다."
    )
    public ResponseEntity<ApiResponse<PageResponse<EditionFeedItemResponseDto>>> getEditionFeed(
            @Parameter(description = "에디션명·닉네임 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "LATEST") CommunityFeedService.SortBy sort,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, PageResponse.of(
                communityFeedService.getFeed(keyword, sort, pageable), EditionFeedItemResponseDto::from));
    }

    @GetMapping("/editions/{conceptId}")
    @Operation(
            summary = "공개 에디션 카드 상세",
            description = "목록과 달리 3D 모델(GLB)까지 함께 내려줍니다. 공개되지 않은 카드는 존재를 알리지 않으려고 404로 답합니다."
    )
    public ResponseEntity<ApiResponse<EditionCardDetailResponseDto>> getEditionCard(
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, EditionCardDetailResponseDto.from(
                communityFeedService.getCard(conceptId)));
    }

    @GetMapping("/collections")
    @Operation(
            summary = "공개 옷장(사용자) 목록",
            description = "컬렉션 전체를 공개한 사용자 목록입니다. 카드가 아니라 사람을 돌려줍니다. "
                    + "카드를 둘러보려면 GET /community/editions를 쓰세요."
    )
    public ResponseEntity<ApiResponse<PageResponse<PublicCollectionResponseDto>>> getPublicCollections(
            @Parameter(description = "닉네임 검색어") @RequestParam(required = false) String nickname,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, PageResponse.of(
                publicSettingService.getPublicFeed(nickname, pageable), PublicCollectionResponseDto::from));
    }

    @GetMapping("/shared/{shareToken}")
    @Operation(
            summary = "공유 링크로 공개 콘텐츠 조회",
            description = "카드 앞면(콘셉트 이미지)과 뒷면(보증서)을 함께 내려줍니다. 보증서가 발급된(최종 확정된) 카드만 노출됩니다."
    )
    public ResponseEntity<ApiResponse<SharedViewResponseDto>> getSharedView(
            @Parameter(description = "공유 토큰") @PathVariable String shareToken,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        PublicSetting setting = sharedViewService.getSetting(shareToken);
        PageResponse<SharedCardResponseDto> cards = PageResponse.of(
                sharedViewService.getSharedCards(setting, pageable), SharedCardResponseDto::from);
        return ApiResponse.success(SuccessCode.GET_SUCCESS, new SharedViewResponseDto(
                setting.getTargetType().name(), setting.getUser().getNickname(), cards));
    }
}
