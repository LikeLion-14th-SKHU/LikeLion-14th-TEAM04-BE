package com.memory_atelier.user.api.dto.response;


import com.memory_atelier.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "회원 공개 프로필 페이지 응답")
public record UserListResponseDto (
    @Schema(description = "현재 페이지의 회원 목록")
    List<UserPublicProfileResponseDto> users,

    @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
    int currentPage,

    @Schema(description = "전체 페이지 수", example = "3")
    int totalPages,

    @Schema(description = "전체 회원 수", example = "42")
    long totalElements,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {
    public static UserListResponseDto from(Page<User> userPage) {
        List<UserPublicProfileResponseDto> userInfoList = userPage.getContent().stream()
                .map(UserPublicProfileResponseDto::from)
                .toList();

        return new UserListResponseDto(
                userInfoList,
                userPage.getNumber(),
                userPage.getTotalPages(),
                userPage.getTotalElements(),
                userPage.hasNext()
        );
    }
}
