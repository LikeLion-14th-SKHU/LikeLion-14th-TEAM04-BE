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

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean anonymized = false;

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

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }
}