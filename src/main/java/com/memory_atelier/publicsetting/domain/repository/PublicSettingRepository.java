package com.memory_atelier.publicsetting.domain.repository;

import com.memory_atelier.publicsetting.domain.PublicSetting;
import com.memory_atelier.publicsetting.domain.PublicSettingTargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicSettingRepository extends JpaRepository<PublicSetting, Long> {

    Optional<PublicSetting> findByUserUserIdAndScopeKey(Long userId, long scopeKey);

    List<PublicSetting> findAllByUserUserIdAndTargetType(Long userId, PublicSettingTargetType targetType);

    // 공유 뷰는 소유자 닉네임과(카드 단위라면) 콘셉트까지 읽으므로 함께 가져온다
    // 컬렉션 설정은 concept가 null이라 left join이어야 한다
    @Query("""
            select s from PublicSetting s
            join fetch s.user
            left join fetch s.concept
            where s.shareToken = :shareToken
            """)
    Optional<PublicSetting> findByShareToken(@Param("shareToken") String shareToken);

    // 공개 옷장(사람) 목록. 닉네임 필터와 탈퇴 사용자 제외를 모두 쿼리에서 처리한다
    @Query("""
            select s from PublicSetting s
            join fetch s.user u
            where s.targetType = com.memory_atelier.publicsetting.domain.PublicSettingTargetType.ALL_COLLECTION
              and s.isPublic = true
              and u.deletedAt is null
              and lower(u.nickname) like lower(concat('%', :keyword, '%'))
            order by s.publicSettingId desc
            """)
    Page<PublicSetting> findPublicGalleries(@Param("keyword") String keyword, Pageable pageable);
}
