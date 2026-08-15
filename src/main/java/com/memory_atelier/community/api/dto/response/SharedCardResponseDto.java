package com.memory_atelier.community.api.dto.response;

import com.memory_atelier.certificate.api.dto.response.CertificateViewDto;
import com.memory_atelier.community.application.SharedViewService.SharedCard;
import com.memory_atelier.edition.domain.EditionConcept;
import io.swagger.v3.oas.annotations.media.Schema;

// 공유 링크로 노출되는 전시 카드 한 장. 앞면은 콘셉트 이미지, 뒷면은 보증서다
// 원본 옷 사진은 인증 없이 열리는 링크라 담지 않는다
@Schema(description = "공유 전시 카드")
public record SharedCardResponseDto(
        @Schema(description = "콘셉트 id", example = "1")
        Long conceptId,

        @Schema(description = "[앞면] 콘셉트 이미지 URL")
        String imageUrl,

        @Schema(description = "[앞면] 목록용 2D 이미지. 정면 PNG가 없으면 콘셉트 이미지로 폴백한다.")
        String gridImageUrl,

        @Schema(description = "[앞면] 3D 모델(GLB) URL. 변환이 끝나기 전에는 null이며, 이때는 2D 이미지로 보여주세요.")
        String modelUrl,

        @Schema(description = "[뒷면] 보증서")
        CertificateViewDto certificate,

        @Schema(description = "좋아요 수", example = "12")
        long likeCount
) {
    public static SharedCardResponseDto from(SharedCard card) {
        EditionConcept concept = card.certificate().getConcept();
        return new SharedCardResponseDto(
                concept.getConceptId(),
                concept.getImageUrl(),
                concept.resolveGridImageUrl(),
                concept.getModelUrl(),
                CertificateViewDto.from(card.certificate()),
                card.likeCount());
    }
}
