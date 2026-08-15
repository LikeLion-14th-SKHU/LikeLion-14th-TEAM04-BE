package com.memory_atelier.community.application;

import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.community.domain.repository.CommunityFeedRepository;
import com.memory_atelier.community.domain.repository.LikeRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.publicsetting.application.PublicSettingService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 공개 에디션 피드. 다른 사람들이 공개한 전시 카드를 정렬·검색해 보여준다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityFeedService {

    private final CommunityFeedRepository feedRepository;
    private final LikeRepository likeRepository;
    private final PublicSettingService publicSettingService;

    // 정렬 기준. 화면의 "인기 에디션 / 최신 에디션" 두 줄에 대응한다
    public enum SortBy {
        POPULAR,
        LATEST
    }

    // 공개된 전시 카드 페이지. 페이지 크기와 무관하게 쿼리 수가 고정이다
    // id 페이지 1회, 총 개수 1회, 카드 내용 1회, 좋아요 집계 1회
    public Page<FeedCard> getFeed(String keyword, SortBy sortBy, Pageable pageable) {
        String normalized = (keyword == null) ? "" : keyword.trim();
        List<Long> certificateIds = (sortBy == SortBy.POPULAR)
                ? feedRepository.findPopularCertificateIds(normalized, pageable)
                : feedRepository.findLatestCertificateIds(normalized, pageable);
        if (certificateIds.isEmpty()) {
            // 뒤따르는 조회들이 빈 IN 절을 받지 않도록 여기서 끊는다
            return new PageImpl<>(List.of(), pageable, feedRepository.countVisibleCertificates(normalized));
        }
        return new PageImpl<>(assemble(certificateIds), pageable, feedRepository.countVisibleCertificates(normalized));
    }

    // 피드에서 카드 한 장을 연다. 공개되지 않은 카드는 존재 자체를 알리지 않으려고 404로 막는다
    public FeedCard getCard(Long conceptId) {
        Certificate certificate = feedRepository.findWithOwnerByConceptId(conceptId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONCEPT_NOT_PUBLIC));
        if (!publicSettingService.isPubliclyVisible(certificate.getConcept())) {
            throw new CustomException(ErrorCode.CONCEPT_NOT_PUBLIC);
        }
        long likeCount = likeRepository.countByConceptConceptId(conceptId);
        return new FeedCard(certificate, likeCount);
    }

    private List<FeedCard> assemble(List<Long> certificateIds) {
        Map<Long, Certificate> certificatesById = feedRepository.findAllWithOwnerByIdIn(certificateIds).stream()
                .collect(Collectors.toMap(Certificate::getCertificateId, Function.identity()));
        List<Long> conceptIds = certificatesById.values().stream()
                .map(cert -> cert.getConcept().getConceptId())
                .toList();
        Map<Long, Long> likeCounts = likeRepository.countByConceptIdIn(conceptIds).stream()
                .collect(Collectors.toMap(
                        LikeRepository.ConceptLikeCount::getConceptId, LikeRepository.ConceptLikeCount::getLikeCount));

        return certificateIds.stream()
                .map(certificatesById::get)
                .filter(Objects::nonNull)
                .map(cert -> new FeedCard(cert, likeCounts.getOrDefault(cert.getConcept().getConceptId(), 0L)))
                .toList();
    }

    // 피드 카드 한 장. 앞면은 콘셉트 이미지, 뒷면은 보증서다
    public record FeedCard(Certificate certificate, long likeCount) {
    }
}
