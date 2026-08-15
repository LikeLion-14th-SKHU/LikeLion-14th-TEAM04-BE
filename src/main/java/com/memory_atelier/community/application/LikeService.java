package com.memory_atelier.community.application;

import com.memory_atelier.community.domain.Like;
import com.memory_atelier.community.domain.repository.LikeRepository;
import com.memory_atelier.edition.application.EditionConceptService;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.publicsetting.application.PublicSettingService;
import com.memory_atelier.user.application.UserService;
import com.memory_atelier.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserService userService;
    private final EditionConceptService editionConceptService;
    private final PublicSettingService publicSettingService;

    @Transactional
    public void like(Long userId, Long conceptId) {
        User user = userService.getActiveUser(userId);
        EditionConcept concept = getVisibleConcept(userId, conceptId);
        if (likeRepository.existsByUserUserIdAndConceptConceptId(userId, conceptId)) {
            throw new CustomException(ErrorCode.ALREADY_LIKED);
        }
        try {
            likeRepository.saveAndFlush(Like.builder().user(user).concept(concept).build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 먼저 저장한 경우. 유니크 제약이 막아 주므로 도메인 에러로 바꿔 준다
            throw new CustomException(ErrorCode.ALREADY_LIKED);
        }
    }

    @Transactional
    public void unlike(Long userId, Long conceptId) {
        likeRepository.findByUserUserIdAndConceptConceptId(userId, conceptId)
                .ifPresent(likeRepository::delete);
    }

    // 좋아요 수는 비로그인 방문자에게도 보여야 한다 — 공개 피드의 카드마다 하트 개수가 붙기 때문이다
    public long countLikes(Long userId, Long conceptId) {
        getVisibleConcept(userId, conceptId);
        return likeRepository.countByConceptConceptId(conceptId);
    }

    // 비로그인 방문자에게는 "내가 눌렀는지"라는 개념이 없으므로 항상 false다
    public boolean isLikedByUser(Long userId, Long conceptId) {
        return userId != null && likeRepository.existsByUserUserIdAndConceptConceptId(userId, conceptId);
    }

    // 공개된 카드이거나 본인 소유일 때만 접근을 허용한다.
    // 그래야 ID를 훑는 것만으로 비공개 카드의 존재와 좋아요 수가 드러나지 않는다.
    // userId가 null이면 비로그인 방문자다
    private EditionConcept getVisibleConcept(Long userId, Long conceptId) {
        EditionConcept concept = editionConceptService.getConceptWithOwner(conceptId);
        boolean isOwner = userId != null && concept.isOwnedBy(userId);
        if (!isOwner && !publicSettingService.isPubliclyVisible(concept)) {
            throw new CustomException(ErrorCode.CONCEPT_NOT_PUBLIC);
        }
        return concept;
    }
}
