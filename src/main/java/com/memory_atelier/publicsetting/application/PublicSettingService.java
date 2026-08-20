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
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicSettingService {

    private final PublicSettingRepository publicSettingRepository;
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

    // 공유 토큰으로 공개 설정을 찾는다. 비공개 토큰이면 존재하지 않는 것처럼 거절한다.
    // 탈퇴한 사용자여도 막지 않는다 — 콘텐츠는 유지하고 닉네임만 가리는 정책이라
    // (User.displayNickname 참고) 여기서 걸러내면 그 정책과 어긋난다
    public PublicSetting getPublicByShareToken(String shareToken) {
        PublicSetting setting = publicSettingRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new CustomException(ErrorCode.PUBLIC_SETTING_NOT_FOUND));
        if (!setting.isPublic()) {
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

    // 같은 트랜잭션에서 조회 후 없으면 바로 생성한다. 예전에는 생성을 REQUIRES_NEW로 별도
    // 트랜잭션에 분리하고 실패 시 재조회하는 방식이었는데, MySQL 기본 격리수준(REPEATABLE READ)
    // 에서는 바깥 트랜잭션이 애초에 자기 시작 시점 스냅샷에 갇혀있어서, 안쪽 트랜잭션이 방금
    // 커밋한 행을 재조회해도 안 보여 매번 "생성 실패"로 잘못 떨어졌다(첫 토글이 무조건 실패하는
    // 버그였음). 같은 트랜잭션 안에서 저장한 엔티티를 그대로 쓰면 이 문제가 없다.
    // 아주 드문 동시 최초 생성 경합은 유니크 제약 위반(DataIntegrityViolationException)으로
    // 자연스럽게 실패하고, GlobalExceptionHandler가 409로 응답한다 — 호출자가 재시도하면 된다
    private PublicSetting getOrCreate(Long userId, PublicSettingTargetType targetType, EditionConcept concept) {
        long scopeKey = (concept != null) ? concept.getConceptId() : PublicSetting.COLLECTION_SCOPE_KEY;
        return publicSettingRepository.findByUserUserIdAndScopeKey(userId, scopeKey)
                .orElseGet(() -> create(userId, targetType, concept));
    }

    private PublicSetting create(Long userId, PublicSettingTargetType targetType, EditionConcept concept) {
        User user = userService.getActiveUser(userId);
        return publicSettingRepository.saveAndFlush(
                PublicSetting.builder()
                        .user(user)
                        .targetType(targetType)
                        .concept(concept)
                        .shareToken(UUID.randomUUID().toString())
                        .build());
    }
}
