package com.memory_atelier.user.api;


import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.user.api.dto.request.UserUpdateRequestDto;
import com.memory_atelier.user.api.dto.response.UserInfoResponseDto;
import com.memory_atelier.user.api.dto.response.UserListResponseDto;
import com.memory_atelier.user.api.dto.response.UserPublicProfileResponseDto;
import com.memory_atelier.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "회원(User) API", description = "내 정보 조회·수정·탈퇴 및 회원 검색 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = "Authorization 헤더의 JWT 액세스 토큰으로 현재 로그인한 사용자 본인의 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<UserInfoResponseDto>> userFindMe(
            @AuthenticationPrincipal Long userId) {
        UserInfoResponseDto responseDto = userService.userFindMe(userId);
        return ApiResponse.success(SuccessCode.GET_SUCCESS, responseDto);
    }

    @PatchMapping("/me")
    @Operation(
            summary = "내 정보 수정",
            description = "현재 로그인한 사용자 본인의 닉네임을 수정합니다. 프로필 이미지는 이 API가 아니라 "
                    + "`PATCH /me/profile-image`로 별도 업로드합니다."
    )
    public ResponseEntity<UserInfoResponseDto> updateMe(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, requestDto));
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자 본인을 탈퇴 처리합니다(소프트 삭제). 성공 시 204 No Content를 반환합니다."
    )
    public ResponseEntity<Void> withdrawMe(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "프로필 이미지 수정",
            description = "이미지 파일을 S3에 업로드하고 그 결과 URL로 프로필 이미지를 교체합니다. "
                    + "기존 이미지가 있으면 교체 후 S3에서 삭제합니다."
    )
    public ResponseEntity<UserInfoResponseDto> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file) {
        UserInfoResponseDto response = userService.updateProfileImage(userId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(
            summary = "닉네임으로 회원 검색",
            description = "닉네임에 검색어가 포함된 회원을 페이지 단위로 조회합니다. "
                    + "탈퇴한 회원은 제외되며, 각 회원은 닉네임·프로필 이미지 등 공개 정보만 노출됩니다. 인증 없이 호출 가능합니다."
    )
    public ResponseEntity<UserListResponseDto> searchByNickname(
            @Parameter(description = "검색할 닉네임(부분 일치)", example = "구름")
            @RequestParam String nickname,
            @Parameter(description = "페이지 번호(0부터 시작)·크기·정렬 기준", example = "page=0&size=20")
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.searchByNickname(nickname, pageable));
    }

    @GetMapping("/{userId}/public-profile")
    @Operation(
            summary = "회원 공개 프로필 단건 조회",
            description = "userId로 특정 회원의 공개 프로필(닉네임, 프로필 이미지)만 조회합니다. "
                    + "이메일 등 민감 정보는 포함되지 않으며, 인증 없이 호출 가능합니다."
    )
    public ResponseEntity<UserPublicProfileResponseDto> getPublicProfile(
            @Parameter(description = "조회할 회원 id", example = "1")
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(userService.getPublicProfile(userId));
    }
}
