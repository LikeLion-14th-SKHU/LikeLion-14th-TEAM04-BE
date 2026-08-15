package com.memory_atelier.user.api;

import com.memory_atelier.user.api.dto.request.UserUpdateRequestDto;
import com.memory_atelier.user.api.dto.response.UserInfoResponseDto;
import com.memory_atelier.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "회원 관리(Admin) API", description = "관리자 전용 회원 조회·수정·탈퇴 API. 모든 API는 ADMIN 권한이 필요합니다.")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(
            summary = "회원 단건 조회 (관리자)",
            description = "userId로 특정 회원의 전체 정보(이메일, 권한 등 포함)를 조회합니다. ADMIN 권한이 필요합니다."
    )
    public ResponseEntity<UserInfoResponseDto> getUserInfo(
            @Parameter(description = "조회할 회원 id", example = "1")
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @PatchMapping("/{userId}")
    @Operation(
            summary = "회원 정보 수정 (관리자)",
            description = "userId로 특정 회원의 닉네임을 수정합니다. ADMIN 권한이 필요합니다."
    )
    public ResponseEntity<UserInfoResponseDto> updateUser(
            @Parameter(description = "수정할 회원 id", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, requestDto));
    }

    @DeleteMapping("/{userId}")
    @Operation(
            summary = "회원 탈퇴 처리 (관리자)",
            description = "userId로 특정 회원을 탈퇴 처리합니다(소프트 삭제). ADMIN 권한이 필요합니다. 성공 시 204 No Content를 반환합니다."
    )
    public ResponseEntity<Void> withdraw(
            @Parameter(description = "탈퇴 처리할 회원 id", example = "1")
            @PathVariable Long userId
    ) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }
}
