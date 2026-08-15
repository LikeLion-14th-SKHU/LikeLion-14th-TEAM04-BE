package com.memory_atelier.certificate.api.dto.response;

import com.memory_atelier.certificate.domain.Certificate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "디지털 보증서 단건 조회 응답")
public record CertificateResponseDto(
        @Schema(description = "콘셉트 id", example = "1")
        Long conceptId,

        @Schema(description = "보증서 내용")
        CertificateViewDto certificate
) {
    public static CertificateResponseDto from(Certificate certificate) {
        return new CertificateResponseDto(
                certificate.getConcept().getConceptId(), CertificateViewDto.from(certificate));
    }
}
