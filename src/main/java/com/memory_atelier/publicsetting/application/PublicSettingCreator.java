package com.memory_atelier.publicsetting.application;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.publicsetting.domain.PublicSetting;
import com.memory_atelier.publicsetting.domain.PublicSettingTargetType;
import com.memory_atelier.publicsetting.domain.repository.PublicSettingRepository;
import com.memory_atelier.user.domain.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 공개 설정 행 생성 전용. 동시 요청이 같은 스코프의 설정을 만들려 하면 유니크 제약에 걸려
// 이 트랜잭션만 롤백되고, 호출자는 기존 행을 다시 조회하면 된다
@Component
@RequiredArgsConstructor
public class PublicSettingCreator {

    private final PublicSettingRepository publicSettingRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(User user, PublicSettingTargetType targetType, EditionConcept concept) {
        publicSettingRepository.saveAndFlush(
                PublicSetting.builder()
                        .user(user)
                        .targetType(targetType)
                        .concept(concept)
                        .shareToken(UUID.randomUUID().toString())
                        .build());
    }
}
