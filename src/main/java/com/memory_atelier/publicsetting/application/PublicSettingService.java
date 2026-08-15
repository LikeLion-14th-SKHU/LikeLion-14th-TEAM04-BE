package com.memory_atelier.publicsetting.application;

import com.memory_atelier.edition.application.EditionConceptService;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.publicsetting.domain.PublicSetting;
import com.memory_atelier.publicsetting.domain.PublicSettingTargetType;
import com.memory_atelier.publicsetting.domain.repository.PublicSettingRepository;
import com.memory_atelier.user.application.UserService;
import com.memory_atelier.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicSettingService {

    private final PublicSettingRepository publicSettingRepository;
    private final PublicSettingCreator publicSettingCreator;
    private final UserService userService;
    private final EditionConceptService editionConceptService;

    // 내 공개 설정 전체를 읽는다. 읽기 전용이 아닌 이유는, 컬렉션 설정을 한 번도 토글하지 않은
    // 사용자에게도 공유 토큰을 내려줘야 하기 때문이다 — 설정 행이 없으면 여기서 기본(비공개)
    // 상태로 만들어 줘야 프론트가 "공유 링크 복사"를 항상 그릴 수 있다
    @Transactional
    public MySettings getMySettings(Long userId) {
        return new MySettings(
                getOrCreate(userId, PublicSettingTargetType.ALL_COLLECTION, null),
                publicSettingRepository.findAllByUserUserIdAndTargetType(userId, PublicSettingTargetType.SINGLE_CARD));
    }

    // 컬렉션 전체 설정 하나와, 따로 지정한 카드 설정들
    public record MySettings(PublicSetting collection, List<PublicSetting> cards) {
    }

    @Transactional
    public PublicSetting toggleCollection(Long userId, boolean isPublic) {
        PublicSetting setting = getOrCreate(userId, PublicSettingTargetType.ALL_COLLECTION, null);
        setting.updatePublic(isPublic);
        return setting;
    }

    @Transactional
    public PublicSetting toggleCard(Long userId, Long conceptId, boolean isPublic) {
        EditionConcept concept = editionConceptService.getOwnedConcept(userId, conceptId);
        PublicSetting setting = getOrCreate(userId, PublicSettingTargetType.SINGLE_CARD, concept);
        setting.updatePublic(isPublic);
        return setting;
    }

    private PublicSetting getOrCreate(Long userId, PublicSettingTargetType targetType, EditionConcept concept) {
        long scopeKey = (concept != null) ? concept.getConceptId() : PublicSetting.COLLECTION_SCOPE_KEY;
        return publicSettingRepository.findByUserUserIdAndScopeKey(userId, scopeKey)
                .orElseGet(() -> create(userId, targetType, concept, scopeKey));
    }

    private PublicSetting create(Long userId, PublicSettingTargetType targetType, EditionConcept concept, long scopeKey) {
        User user = userService.getActiveUser(userId);
        try {
            publicSettingCreator.create(user, targetType, concept);
        } catch (DataAccessException | UnexpectedRollbackException e) {
            // 동시 요청이 먼저 만들었다는 뜻이므로 아래에서 그 행을 다시 읽으면 된다
            log.debug("공개 설정이 이미 존재합니다: userId={}, scopeKey={}", userId, scopeKey);
        }
        return publicSettingRepository.findByUserUserIdAndScopeKey(userId, scopeKey)
                .orElseThrow(() -> new CustomException(ErrorCode.CONFLICT, "공개 설정 생성에 실패했습니다. 다시 시도해 주세요."));
    }
}
