package com.memory_atelier.publicsetting.domain;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.global.entity.BaseTimeEntity;
import com.memory_atelier.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "public_settings",
        uniqueConstraints = {
                // 사용자당 스코프별로 설정은 하나만 존재해야 한다
                @UniqueConstraint(columnNames = {"user_id", "scope_key"}),
                @UniqueConstraint(columnNames = "share_token")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicSetting extends BaseTimeEntity {

    // ALL_COLLECTION 스코프를 나타내는 scopeKey 값
    public static final long COLLECTION_SCOPE_KEY = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "public_setting_id")
    private Long publicSettingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicSettingTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private EditionConcept concept;

    // 유니크 제약을 걸기 위한 스코프 식별자. 컬렉션 전체는 0, 카드 단위는 콘셉트 id
    // (concept_id는 컬렉션 설정에서 null이라 null끼리는 중복 판정이 되지 않는다)
    @Column(name = "scope_key", nullable = false)
    private long scopeKey;

    @Column(nullable = false)
    private boolean isPublic;

    @Column(name = "share_token", nullable = false)
    private String shareToken;

    @Builder
    private PublicSetting(User user, PublicSettingTargetType targetType, EditionConcept concept, String shareToken) {
        this.user = user;
        this.targetType = targetType;
        this.concept = concept;
        this.scopeKey = (concept != null) ? concept.getConceptId() : COLLECTION_SCOPE_KEY;
        this.shareToken = shareToken;
        this.isPublic = false;
    }

    public void updatePublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
