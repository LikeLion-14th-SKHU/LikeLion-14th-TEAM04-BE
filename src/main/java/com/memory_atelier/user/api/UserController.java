package com.memory_atelier.user.api;


import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.user.api.dto.request.UserSaveRequestDto;
import com.memory_atelier.user.api.dto.request.UserUpdateRequestDto;
import com.memory_atelier.user.api.dto.response.UserInfoResponseDto;
import com.memory_atelier.user.api.dto.response.UserListResponseDto;
import com.memory_atelier.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "JWT 토큰으로 현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserInfoResponseDto>> userFindMe(
            @AuthenticationPrincipal Long userId) {
        UserInfoResponseDto responseDto = userService.userFindMe(userId);
        return ApiResponse.success(SuccessCode.GET_SUCCESS, responseDto);
    }

    // 내 정보 수정
    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "JWT 토큰으로 현재 로그인한 사용자의 정보를 수정합니다.")
    public ResponseEntity<UserInfoResponseDto> updateMe(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, requestDto));
    }

    // 회원 탈퇴 (본인)
    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "JWT 토큰으로 현재 로그인한 사용자를 탈퇴 처리합니다.")
    public ResponseEntity<Void> withdrawMe(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }

    // 회원가입
    @PostMapping
    public ResponseEntity<Long> signup(@Valid @RequestBody UserSaveRequestDto requestDto) {
        Long userId = userService.signUp(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    // 닉네임으로 회원 검색 (인증 필요, role 제한 없음 - 공개 프로필 정보만 반환)
    @GetMapping("/search")
    @Operation(summary = "닉네임 검색", description = "닉네임에 검색어가 포함된 회원 목록을 조회합니다.")
    public ResponseEntity<UserListResponseDto> searchByNickname(
            @RequestParam String nickname,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.searchByNickname(nickname, pageable));
    }

    // 단건 회원 조회 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponseDto> getUserInfo(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    // 회원 정보 수정 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserInfoResponseDto> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, requestDto));
    }

    // 회원 탈퇴 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> withdraw(@PathVariable Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }

    // 프로필 이미지 수정
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserInfoResponseDto> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        UserInfoResponseDto response = userService.updateProfileImage(userId, file);
        return ResponseEntity.ok(response);
    }
}
