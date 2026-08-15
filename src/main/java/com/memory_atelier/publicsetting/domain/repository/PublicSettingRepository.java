package com.memory_atelier.publicsetting.domain.repository;

import com.memory_atelier.publicsetting.domain.PublicSetting;
import com.memory_atelier.publicsetting.domain.PublicSettingTargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicSettingRepository extends JpaRepository<PublicSetting, Long> {

    Optional<PublicSetting> findByUserUserIdAndScopeKey(Long userId, long scopeKey);

    List<PublicSetting> findAllByUserUserIdAndTargetType(Long userId, PublicSettingTargetType targetType);
}
