package com.memory_atelier.certificate.application;

import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.certificate.domain.repository.CertificateRepository;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final EditionNumberIssuer editionNumberIssuer;

    // 확정된 콘셉트에 보증서를 발급한다. 표시에 필요한 값을 전부 발급 시점 스냅샷으로 복사한다 —
    // 원본을 라이브로 참조하면 사용자가 나중에 에디션명을 바꾸거나 추억을 수정했을 때 이미 발급된
    // 보증서 내용이 따라 바뀌어 "보증서 불변" 원칙이 깨진다
    //
    // 배치당 하나라는 보장은 certificates.generation_id 유니크 제약이 한다. 아래 존재 확인은
    // 흔한 경우를 미리 걸러줄 뿐, 같은 배치의 서로 다른 콘셉트를 동시에 확정하면 둘 다 통과하므로
    // 그것만으로는 부족하다. 호출자(CertificateIssueFacade)의 트랜잭션에 참여해서, 확정 표시와
    // 발급이 함께 성립하거나 함께 없던 일이 되게 한다
    @Transactional
    public Certificate issue(EditionConcept concept) {
        EditionGeneration generation = concept.getGeneration();
        if (certificateRepository.existsByGenerationGenerationId(generation.getGenerationId())) {
            throw new CustomException(ErrorCode.CERTIFICATE_ALREADY_ISSUED);
        }
        String editionNumber = editionNumberIssuer.issue(concept.getCategory());
        try {
            return certificateRepository.saveAndFlush(
                    Certificate.builder()
                            .concept(concept)
                            .generation(generation)
                            .editionNumber(editionNumber)
                            .editionName(generation.getEditionName())
                            .category(concept.getCategory())
                            .certificateText(generation.getCertificateText())
                            .storySnapshot(generation.getStorySnapshot())
                            .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 먼저 발급했다는 뜻이다. 이 트랜잭션은 롤백되므로 방금 소비한 채번 행도
            // 함께 되돌아간다 — 번호가 비는 대신 그 값은 그냥 다음 요청이 다시 받아 간다
            throw new CustomException(ErrorCode.CERTIFICATE_ALREADY_ISSUED);
        }
    }

    // 보증서 단건. 소유하지 않았거나 존재하지 않는 보증서는 구분 없이 404로 거절한다(소유권 은닉 정책)
    public Certificate getOwnedCertificate(Long userId, Long conceptId) {
        Certificate certificate = certificateRepository.findByConceptId(conceptId)
                .orElseThrow(() -> new CustomException(ErrorCode.CERTIFICATE_NOT_FOUND));
        if (!certificate.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.CERTIFICATE_NOT_FOUND);
        }
        return certificate;
    }

    // 내 컬렉션에 전시되는 카드 목록. 확정되지 않은 후보는 보증서가 없어 여기에 오르지 않는다
    public Page<Certificate> getMyCertificates(Long userId, Pageable pageable) {
        return certificateRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId, pageable);
    }

    // 콘셉트 id로 보증서를 찾는다. 소유권을 확인하지 않고, 없으면 빈 값을 반환한다
    // (커뮤니티 공유 뷰에서 "아직 확정 전이라 보여줄 게 없다"를 에러가 아니라 빈 결과로 다루기 위함)
    public Optional<Certificate> findByConceptId(Long conceptId) {
        return certificateRepository.findByConceptId(conceptId);
    }

    // 카드 단위로 비공개 처리된 콘셉트를 제외한 컬렉션 목록(공유 링크·공개 옷장에서 사용)
    public Page<Certificate> getCertificatesExcluding(Long ownerId, Set<Long> excludedConceptIds, Pageable pageable) {
        return excludedConceptIds.isEmpty()
                ? certificateRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                : certificateRepository.findAllByOwnerIdAndConceptIdNotInOrderByCreatedAtDesc(
                        ownerId, excludedConceptIds, pageable);
    }
}
