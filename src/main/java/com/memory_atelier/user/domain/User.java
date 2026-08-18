package com.memory_atelier.user.domain;

import com.memory_atelier.global.entity.BaseTimeEntity;
import com.memory_atelier.user.api.dto.request.UserUpdateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_id"})
        }
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Provider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 255)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    // 콘셉트 열람·에디션 재생성에 쓰는 잔액. 조회·표시용으로만 읽는다
    // 실제 차감은 CreditService가 조건부 UPDATE로 원자적으로 하므로, 이 필드를 읽어서 빼면 안 된다
    @Column(nullable = false)
    private int credit = 0;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean anonymized = false;

    @Column(length = 30)
    private String collectionName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CollectionTheme collectionTheme = CollectionTheme.WHITE;

    @Builder
    private User(Provider provider, String providerId, String nickname, String email, String password, String profileImageUrl) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.role = Role.USER;
        this.emailVerified = false;
        this.anonymized = false;
    }

    public void update(UserUpdateRequestDto userUpdateRequestDto) {
        this.nickname = userUpdateRequestDto.nickname();
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void updateCollectionTheme(CollectionTheme collectionTheme) {
        this.collectionTheme = collectionTheme;
    }

    // 신규가입 지급·관리자 지급 공통 진입점
    // 차감은 이 메서드가 아니라 CreditService가 담당한다
    public void grantCredit(int amount) {
        this.credit += amount;
    }

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }
}