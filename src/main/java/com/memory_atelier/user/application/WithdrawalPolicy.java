package com.memory_atelier.user.application;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 탈퇴 유예·이메일 쿨다운 정책. 새 컬럼 없이 User.deletedAt(탈퇴 신청 시각) 하나로 전부 계산한다
// deletedAt ~ +gracePeriodDays: 유예 기간(재로그인하면 자동 취소)
// +gracePeriodDays 이후: 확정 탈퇴(재로그인 불가)
// +gracePeriodDays+emailCooldownDays 이후: 같은 이메일로 재가입 가능
@Component
public class WithdrawalPolicy {

    private final int gracePeriodDays;
    private final int emailCooldownDays;

    public WithdrawalPolicy(
            @Value("${app.withdrawal.grace-period-days}") int gracePeriodDays,
            @Value("${app.withdrawal.email-cooldown-days}") int emailCooldownDays) {
        this.gracePeriodDays = gracePeriodDays;
        this.emailCooldownDays = emailCooldownDays;
    }

    // 탈퇴 신청 직후 ~ 유예 기간 안이면 true (로그인 시 자동 재활성화 대상)
    public boolean isWithinGracePeriod(LocalDateTime deletedAt) {
        return deletedAt != null && deletedAt.plusDays(gracePeriodDays).isAfter(LocalDateTime.now());
    }

    // 재가입 차단 쿼리에 쓸 기준 시각. deletedAt이 이 시각보다 이후면 아직 쿨다운 중이다
    public LocalDateTime emailBlockCutoff() {
        return LocalDateTime.now().minusDays((long) gracePeriodDays + emailCooldownDays);
    }
}
