package com.memory_atelier.community.api.dto.response;

import com.memory_atelier.certificate.api.dto.response.CertificateViewDto;
import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.community.application.CommunityFeedService.FeedCard;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

// 피드에서 카드 한 장을 열었을 때. 목록과 달리 3D 모델과 보증서 전체를 함께 내려준다
@Schema(description = "공개 에디션 카드 상세")
public record EditionCardDetailResponseDto(
        @Schema(description = "콘셉트 id", example = "1")
        Long conceptId,

        @Schema(description = "[앞면] 콘셉트 이미지")
        String imageUrl,

        @Schema(description = "[앞면] 목록용 2D 이미지")
        String gridImageUrl,

        @Schema(description = "[앞면] 3D 모델(GLB). 변환이 끝나기 전에는 null이며, 이때는 2D 이미지로 보여주세요.")
        String modelUrl,

        @Schema(description = "[뒷면] 보증서")
        CertificateViewDto certificate,

        @Schema(description = "만든 사람 id", example = "1")
        Long ownerId,

        @Schema(description = "만든 사람 닉네임", example = "공개설정오너")
        String ownerNickname,

        @Schema(description = "만든 사람 프로필 이미지")
        String ownerProfileImageUrl,

        @Schema(description = "좋아요 수", example = "45")
        long likeCount
) {
    public static EditionCardDetailResponseDto from(FeedCard card) {
        Certificate certificate = card.certificate();
        EditionConcept concept = certificate.getConcept();
        User owner = concept.getGeneration().getMemory().getUser();
        return new EditionCardDetailResponseDto(
                concept.getConceptId(),
                concept.getImageUrl(),
                concept.resolveGridImageUrl(),
                concept.getModelUrl(),
                CertificateViewDto.from(certificate),
                owner.getUserId(),
                owner.displayNickname(),
                owner.getProfileImageUrl(),
                card.likeCount());
    }
}
