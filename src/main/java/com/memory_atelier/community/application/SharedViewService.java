package com.memory_atelier.community.application;

import com.memory_atelier.certificate.application.CertificateService;
import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.community.domain.repository.LikeRepository;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.publicsetting.application.PublicSettingService;
import com.memory_atelier.publicsetting.domain.PublicSetting;
import com.memory_atelier.publicsetting.domain.PublicSettingTargetType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 공유 토큰으로 노출되는 콘텐츠(컬렉션 전체 또는 카드 한 장)를 조립한다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharedViewService {

    private final PublicSettingService publicSettingService;
    private final CertificateService certificateService;
    private final LikeRepository likeRepository;

    public PublicSetting getSetting(String shareToken) {
        return publicSettingService.getPublicByShareToken(shareToken);
    }

    // 공유 링크로 노출할 카드 페이지.
    // SINGLE_CARD는 카드가 0장 또는 1장뿐이라 페이지 개념이 무의미하지만,
    // 응답 형태를 통일하려고 같은 페이지 요청 범위 안에서만 그 1장을 돌려주는 {@link PageImpl}로 감싼다.
    public Page<SharedCard> getSharedCards(PublicSetting setting, Pageable pageable) {
        Page<Certificate> certificates = getSharedCertificates(setting, pageable);
        if (certificates.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, certificates.getTotalElements());
        }
        List<Long> conceptIds = certificates.getContent().stream()
                .map(c -> c.getConcept().getConceptId())
                .toList();
        Map<Long, Long> likeCounts = likeRepository.countByConceptIdIn(conceptIds).stream()
                .collect(Collectors.toMap(
                        LikeRepository.ConceptLikeCount::getConceptId, LikeRepository.ConceptLikeCount::getLikeCount));

        List<SharedCard> content = certificates.getContent().stream()
                .map(cert -> new SharedCard(cert, likeCounts.getOrDefault(cert.getConcept().getConceptId(), 0L)))
                .toList();
        return new PageImpl<>(content, pageable, certificates.getTotalElements());
    }

    private Page<Certificate> getSharedCertificates(PublicSetting setting, Pageable pageable) {
        if (setting.getTargetType() == PublicSettingTargetType.SINGLE_CARD) {
            EditionConcept concept = setting.getConcept();
            // 카드 단위 공유는 확정(보증서 발급) 전에도 설정해둘 수 있으므로, 보증서가
            // 아직 없으면 에러가 아니라 "보여줄 게 없다"는 뜻으로 빈 페이지를 돌려준다
            List<Certificate> all = certificateService.findByConceptId(concept.getConceptId())
                    .map(List::of)
                    .orElse(List.of());
            List<Certificate> pageContent = (pageable.getPageNumber() == 0) ? all : List.of();
            return new PageImpl<>(pageContent, pageable, all.size());
        }
        Long ownerId = setting.getUser().getUserId();
        Set<Long> hiddenCardIds = publicSettingService.hiddenCardIds(ownerId);
        return certificateService.getCertificatesExcluding(ownerId, hiddenCardIds, pageable);
    }

    // 공유 카드 한 장. 앞면은 콘셉트 이미지, 뒷면은 보증서다
    public record SharedCard(Certificate certificate, long likeCount) {
    }
}
