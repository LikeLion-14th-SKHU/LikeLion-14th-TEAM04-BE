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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 해당 콘셉트가 외부에 공개된 상태인지. 카드 단위 설정이 있으면 그 값이 컬렉션 전체 설정보다 우선한다
    public boolean isPubliclyVisible(EditionConcept concept) {
        if (!concept.isUnlocked()) {
            return false;
        }
        Long ownerId = concept.getGeneration().getMemory().getUser().getUserId();
        return publicSettingRepository.findByUserUserIdAndScopeKey(ownerId, concept.getConceptId())
                .map(PublicSetting::isPublic)
                .orElseGet(() -> publicSettingRepository
                        .findByUserUserIdAndScopeKey(ownerId, PublicSetting.COLLECTION_SCOPE_KEY)
                        .map(PublicSetting::isPublic)
                        .orElse(false));
    }

    // 공유 토큰으로 공개 설정을 찾는다. 비공개거나 탈퇴한 사용자의 토큰이면 존재하지 않는 것처럼 거절한다
    public PublicSetting getPublicByShareToken(String shareToken) {
        PublicSetting setting = publicSettingRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new CustomException(ErrorCode.PUBLIC_SETTING_NOT_FOUND));
        if (!setting.isPublic() || !setting.getUser().isActive()) {
            throw new CustomException(ErrorCode.PUBLIC_SETTING_NOT_FOUND);
        }
        return setting;
    }

    // 컬렉션 전체를 공개한 사용자 목록 — "누구의 옷장을 구경할지" 고르는 화면용
    public Page<PublicSetting> getPublicFeed(String nicknameKeyword, Pageable pageable) {
        String keyword = (nicknameKeyword == null) ? "" : nicknameKeyword.trim();
        return publicSettingRepository.findPublicGalleries(keyword, pageable);
    }

    // 카드 단위로 비공개 처리된 콘셉트 id들. 컬렉션 전체가 공개여도 이 카드들은 제외해야 한다
    public Set<Long> hiddenCardIds(Long ownerId) {
        return publicSettingRepository
                .findAllByUserUserIdAndTargetType(ownerId, PublicSettingTargetType.SINGLE_CARD).stream()
                .filter(setting -> !setting.isPublic())
                .map(PublicSetting::getScopeKey)
                .collect(Collectors.toSet());
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
