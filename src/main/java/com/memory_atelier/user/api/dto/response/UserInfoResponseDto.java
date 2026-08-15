package com.memory_atelier.user.api.dto.response;

import com.memory_atelier.user.domain.User;
import lombok.Builder;

@Builder
public record UserInfoResponseDto(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        boolean emailVerified,
        String role
) {
    public static UserInfoResponseDto from(User user)
    {
        return new UserInfoResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.isEmailVerified(),
                user.getRole().name()
        );

    }
}
