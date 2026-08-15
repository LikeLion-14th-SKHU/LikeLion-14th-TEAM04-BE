package com.memory_atelier.certificate.api.dto.response;

import com.memory_atelier.certificate.domain.Certificate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

// 전시 카드의 뒷면(보증서) 표시 항목. 보증서 단건 조회·컬렉션 카드가 같은 값을 보여주므로 한 곳에서만 정의한다
// 모든 값이 발급 시점 스냅샷이다 — 소유자가 나중에 에디션명을 바꾸거나 추억을 수정해도 이미 발급된 보증서는 따라 바뀌지 않는다
@Schema(description = "전시 카드 뒷면(보증서)")
public record CertificateViewDto(
        @Schema(description = "보증서 id", example = "1")
        Long certificateId,

        @Schema(description = "에디션명", example = "Heritage Edition")
        String editionName,

        @Schema(description = "고유 에디션 번호(시리얼). [카테고리 코드 1글자][전역 순번 8자리, 36진수]", example = "B00000001")
        String editionNumber,

        @Schema(description = "에디션 카테고리", example = "가방")
        String category,

        @Schema(description = "추억 사연(발급 시점 스냅샷)", example = "대학 시절 첫 아르바이트로 산 가방이에요.")
        String story,

        @Schema(description = "AI가 다듬은 보증서 문구")
        String certificateText,

        @Schema(description = "보증서 발급 시각")
        Instant issuedAt
) {
    public static CertificateViewDto from(Certificate certificate) {
        return new CertificateViewDto(
                certificate.getCertificateId(),
                certificate.getEditionName(),
                certificate.getEditionNumber(),
                certificate.getCategory(),
                certificate.getStorySnapshot(),
                certificate.getCertificateText(),
                certificate.getCreatedAt());
    }
}
