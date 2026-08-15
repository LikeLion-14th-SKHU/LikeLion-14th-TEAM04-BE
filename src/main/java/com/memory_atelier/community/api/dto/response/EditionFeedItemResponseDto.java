package com.memory_atelier.community.api.dto.response;

import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.community.application.CommunityFeedService.FeedCard;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

// 공개 피드에 깔리는 에디션 카드 한 장. 목록용이라 3D(GLB)는 싣지 않는다
// 뷰어가 무거워 한 화면에 여러 개를 띄울 수 없기 때문
// 상세로 들어가면 modelUrl이 함께 온다
@Schema(description = "공개 피드의 에디션 카드")
public record EditionFeedItemResponseDto(
        @Schema(description = "콘셉트 id. 상세·좋아요 API에 쓴다.", example = "1")
        Long conceptId,

        @Schema(description = "카드 이미지. 정면 PNG가 없으면 콘셉트 이미지로 폴백한다.")
        String imageUrl,

        @Schema(description = "에디션명", example = "Heritage Edition")
        String editionName,

        @Schema(description = "고유 에디션 번호(시리얼)", example = "B00000001")
        String editionNumber,

        @Schema(description = "에디션 카테고리", example = "가방")
        String category,

        @Schema(description = "만든 사람 id", example = "1")
        Long ownerId,

        @Schema(description = "만든 사람 닉네임. 검색 대상이다.", example = "공개설정오너")
        String ownerNickname,

        @Schema(description = "만든 사람 프로필 이미지")
        String ownerProfileImageUrl,

        @Schema(description = "좋아요 수", example = "45")
        long likeCount,

        @Schema(description = "카드 생성 날짜(보증서 발급 시각)")
        Instant createdAt
) {
    public static EditionFeedItemResponseDto from(FeedCard card) {
        Certificate certificate = card.certificate();
        EditionConcept concept = certificate.getConcept();
        User owner = concept.getGeneration().getMemory().getUser();
        return new EditionFeedItemResponseDto(
                concept.getConceptId(),
                concept.resolveGridImageUrl(),
                certificate.getEditionName(),
                certificate.getEditionNumber(),
                certificate.getCategory(),
                owner.getUserId(),
                owner.getNickname(),
                owner.getProfileImageUrl(),
                card.likeCount(),
                certificate.getCreatedAt());
    }
}
