package com.memory_atelier.certificate.api.dto.response;

import com.memory_atelier.user.domain.CollectionTheme;
import com.memory_atelier.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "컬렉션 이름/테마 설정")
public record CollectionSettingsResponseDto(
        @Schema(description = "컬렉션 이름. 설정하지 않았으면 null", example = "구름이의 옷장")
        String collectionName,

        @Schema(description = "적용된 테마", example = "IVORY")
        CollectionTheme theme
) {
    public static CollectionSettingsResponseDto from(User user) {
        return new CollectionSettingsResponseDto(user.getCollectionName(), user.getCollectionTheme());
    }
}
