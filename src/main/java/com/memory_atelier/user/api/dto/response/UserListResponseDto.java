package com.memory_atelier.user.api.dto.response;


import com.memory_atelier.user.domain.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record UserListResponseDto (
    List<UserInfoResponseDto> users,
    int currentPage,
    int totalPages,
    long totalElements,
    boolean hasNext
) {
    public static UserListResponseDto from(Page<User> userPage) {
        List<UserInfoResponseDto> userInfoList = userPage.getContent().stream()
                .map(UserInfoResponseDto::from)
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
