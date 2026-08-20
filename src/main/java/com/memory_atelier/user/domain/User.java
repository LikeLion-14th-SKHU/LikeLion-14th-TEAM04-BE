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

    private static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자";

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
    }

    // 유예 기간 안에 재로그인해서 탈퇴를 취소할 때 쓴다 — AuthService.login 참고
    public void cancelWithdrawal() {
        this.deletedAt = null;
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }

    // 공개 피드·컬렉션 목록 등 타인에게 노출되는 곳에서 쓴다. 콘텐츠는 그대로 두고
    // 닉네임만 가려서, 탈퇴(유예 기간 포함) 후에도 실제 활동 이력은 보존하되 신원만 감춘다
    public String displayNickname() {
        return isActive() ? nickname : WITHDRAWN_DISPLAY_NAME;
    }

    // email에 유니크 제약이 있어서, 쿨다운이 끝나 같은 이메일로 새로 가입하려는 사람이 있을 때
    // 이 자리를 차지하고 있는 옛 탈퇴 계정의 이메일을 비워준다(AuthService.signup 참고).
    // 스케줄러 없이, 실제로 그 이메일이 다시 필요해지는 시점에만 지연 처리한다
    public void releaseEmailForReuse() {
        this.email = "withdrawn-user-" + userId + "@removed.invalid";
        this.anonymized = true;
    }
}