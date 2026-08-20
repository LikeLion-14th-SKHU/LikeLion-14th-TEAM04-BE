package com.memory_atelier.user.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 크레딧 지급·소모량 정책. 클라이언트가 소모량을 정하지 못하도록 서버 설정에서만 값을 가져온다
@Component
public class CreditPolicy {

    private final int signupGrant;
    private final int editionUnlockCost;
    private final int editionRegenerateCost;

    public CreditPolicy(
            @Value("${app.credit.signup-grant}") int signupGrant,
            @Value("${app.credit.edition-unlock-cost}") int editionUnlockCost,
            @Value("${app.credit.edition-regenerate-cost}") int editionRegenerateCost) {
        this.signupGrant = signupGrant;
        this.editionUnlockCost = editionUnlockCost;
        this.editionRegenerateCost = editionRegenerateCost;
    }

    // 신규 가입 시 자동 지급량
    public int signupGrant() {
        return signupGrant;
    }

    // 잠긴 콘셉트 한 장을 여는 데 드는 크레딧
    public int editionUnlockCost() {
        return editionUnlockCost;
    }

    // 무료 횟수를 넘긴 에디션 재생성 한 회차에 드는 크레딧
    public int editionRegenerateCost() {
        return editionRegenerateCost;
    }
}
