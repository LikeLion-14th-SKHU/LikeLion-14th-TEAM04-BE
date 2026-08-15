package com.memory_atelier.user.api.dto.response;

import com.memory_atelier.user.domain.User;
import lombok.Builder;

@Builder
public record UserPublicProfileResponseDto(
        Long userId,
        String nickname,
        String profileImageUrl
) {
    public static UserPublicProfileResponseDto from(User user) {
        return new UserPublicProfileResponseDto(
                user.getUserId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
