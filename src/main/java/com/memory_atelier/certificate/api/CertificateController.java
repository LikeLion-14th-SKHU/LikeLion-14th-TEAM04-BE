package com.memory_atelier.certificate.api;

import com.memory_atelier.certificate.api.dto.response.CertificateResponseDto;
import com.memory_atelier.certificate.application.CertificateIssueFacade;
import com.memory_atelier.certificate.application.CertificateService;
import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/edition-concepts/{conceptId}")
@Tag(name = "보증서(Certificate) API", description = "최종 확정·디지털 보증서 발급·조회 API")
public class CertificateController {

    private final CertificateIssueFacade certificateIssueFacade;
    private final CertificateService certificateService;

    @PostMapping("/certificate")
    @Operation(
            summary = "최종 확정(보증서 발급)",
            description = "콘셉트를 최종 이미지로 확정하고 디지털 보증서를 발급합니다. 확정과 발급은 한 트랜잭션이라 "
                    + "둘 다 성공하거나 둘 다 없던 일이 됩니다. 3D 모델 변환은 뒤에서 비동기로 이어지므로 응답 직후에는 "
                    + "modelUrl이 비어 있을 수 있고, 그동안 카드는 2D 이미지로 보여주면 됩니다. "
                    + "확정은 같은 생성 배치에서 한 번만 가능하고, 잠긴 콘셉트는 먼저 열람 잠금 해제를 해야 합니다."
    )
    public ResponseEntity<ApiResponse<CertificateResponseDto>> selectFinal(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        return ApiResponse.success(SuccessCode.CREATED, CertificateResponseDto.from(
                certificateIssueFacade.selectFinal(userId, conceptId)));
    }

    @GetMapping("/certificate")
    @Operation(summary = "보증서 조회", description = "확정된 콘셉트의 보증서를 조회합니다. 소유자만 볼 수 있습니다.")
    public ResponseEntity<ApiResponse<CertificateResponseDto>> getCertificate(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "콘셉트 id", example = "1") @PathVariable Long conceptId) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, CertificateResponseDto.from(
                certificateService.getOwnedCertificate(userId, conceptId)));
    }
}
