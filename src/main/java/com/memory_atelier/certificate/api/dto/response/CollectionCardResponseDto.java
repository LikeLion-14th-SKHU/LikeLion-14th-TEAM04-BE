package com.memory_atelier.certificate.api.dto.response;

import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.edition.domain.EditionConcept;
import io.swagger.v3.oas.annotations.media.Schema;

// 내 컬렉션에 전시되는 카드 한 장. 앞면은 콘셉트 이미지, 뒷면은 보증서다
// 컬렉션에는 최종 확정된 카드만 올라오므로 뒷면은 항상 채워진다
@Schema(description = "컬렉션 전시 카드")
public record CollectionCardResponseDto(
        @Schema(description = "콘셉트 id", example = "1")
        Long conceptId,

        @Schema(description = "[앞면] 콘셉트 이미지 URL", example = "https://cdn.example.com/edition/1/concept-1.png")
        String imageUrl,

        @Schema(
                description = "[앞면] 컬렉션 그리드용 2D 이미지. 3D 변환의 투명배경 정면 PNG가 없으면 콘셉트 이미지로 폴백합니다.",
                example = "https://cdn.example.com/models/concept-1_front.png")
        String gridImageUrl,

        @Schema(
                description = "[앞면] 3D 모델(GLB) URL. 변환이 끝나기 전에는 null이며, 이때는 2D 이미지로 보여주세요.",
                example = "https://cdn.example.com/models/concept-1.glb")
        String modelUrl,

        @Schema(description = "[뒷면] 보증서")
        CertificateViewDto certificate
) {
    public static CollectionCardResponseDto from(Certificate certificate) {
        EditionConcept concept = certificate.getConcept();
        return new CollectionCardResponseDto(
                concept.getConceptId(),
                concept.getImageUrl(),
                concept.resolveGridImageUrl(),
                concept.getModelUrl(),
                CertificateViewDto.from(certificate));
    }
}
