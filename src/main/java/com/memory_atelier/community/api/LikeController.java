package com.memory_atelier.community.api;

import com.memory_atelier.community.api.dto.response.LikeStatusResponseDto;
import com.memory_atelier.community.application.LikeService;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 좋아요. 커뮤니티 개념이라 /community 아래에 둔다
// 누르고 취소하는 건 로그인이 필요하지만, 개수 조회는 열려 있다
// 공개 피드의 카드마다 하트 개수가 붙는데 그걸 보려고 로그인을 요구할 수는 없기 때문이다
@RestController
@RequiredArgsConstructor
@RequestMapping("/community/editions/{conceptId}/likes")
@Tag(name = "좋아요(Like) API", description = "에디션 카드 좋아요 API")
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    @Operation(summary = "좋아요 등록")
    public ResponseEntity<ApiResponse<Void>> like(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        likeService.like(userId, conceptId);
        return ApiResponse.successEmpty(SuccessCode.CREATED);
    }

    @DeleteMapping
    @Operation(summary = "좋아요 취소")
    public ResponseEntity<ApiResponse<Void>> unlike(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        likeService.unlike(userId, conceptId);
        return ApiResponse.success();
    }

    @SecurityRequirements
    @GetMapping
    @Operation(
            summary = "좋아요 수/내 여부 조회",
            description = "인증 없이 호출할 수 있습니다. 비로그인 상태면 likeCount는 정상적으로 내려가고 likedByMe는 항상 false입니다."
    )
    public ResponseEntity<ApiResponse<LikeStatusResponseDto>> getStatus(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        long count = likeService.countLikes(userId, conceptId);
        boolean likedByMe = likeService.isLikedByUser(userId, conceptId);
        return ApiResponse.success(SuccessCode.GET_SUCCESS, new LikeStatusResponseDto(count, likedByMe));
    }
}
