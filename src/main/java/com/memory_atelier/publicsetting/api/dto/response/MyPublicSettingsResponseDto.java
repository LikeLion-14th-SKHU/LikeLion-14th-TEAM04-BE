package com.memory_atelier.publicsetting.api.dto.response;

import com.memory_atelier.publicsetting.domain.PublicSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 이 응답이 없으면 토글의 현재 상태와 공유 링크를 알아낼 방법이 PATCH뿐이라, 화면을 그리려면 설정을 바꿔야 한다
// 읽기와 쓰기를 갈라 놓는 게 이 DTO의 목적이다
@Schema(description = "내 공개 설정")
public record MyPublicSettingsResponseDto(
        @Schema(description = "컬렉션 전체 공개 설정. 한 번도 토글하지 않았다면 기본 비공개 상태로 만들어져 내려온다.")
        Scope collection,

        @Schema(description = "카드 단위로 따로 지정한 설정만 담긴다. 여기 없는 카드는 컬렉션 설정을 따른다.")
        List<CardScope> cards
) {
    public static MyPublicSettingsResponseDto of(PublicSetting collection, List<PublicSetting> cards) {
        return new MyPublicSettingsResponseDto(
                Scope.from(collection),
                cards.stream().map(CardScope::from).toList());
    }

    @Schema(description = "공개 범위 한 건")
    public record Scope(
            @Schema(description = "공개 여부", example = "true")
            boolean isPublic,

            @Schema(
                    description = "공유 링크 토큰. GET /community/shared/{shareToken}으로 열립니다. "
                            + "비공개 상태에서는 링크를 열어도 404입니다.",
                    example = "3f2e9c1a-1234-4a5b-9c1a-abcdef123456")
            String shareToken
    ) {
        static Scope from(PublicSetting setting) {
            return new Scope(setting.isPublic(), setting.getShareToken());
        }
    }

    @Schema(description = "카드 단위 공개 범위. 컬렉션 전체 설정보다 우선한다.")
    public record CardScope(
            @Schema(description = "콘셉트 id", example = "1")
            Long conceptId,

            @Schema(description = "공개 여부", example = "false")
            boolean isPublic,

            @Schema(description = "이 카드만 여는 공유 링크 토큰", example = "8a1b2c3d-5678-4e9f-8a1b-fedcba654321")
            String shareToken
    ) {
        static CardScope from(PublicSetting setting) {
            return new CardScope(setting.getScopeKey(), setting.isPublic(), setting.getShareToken());
        }
    }
}
