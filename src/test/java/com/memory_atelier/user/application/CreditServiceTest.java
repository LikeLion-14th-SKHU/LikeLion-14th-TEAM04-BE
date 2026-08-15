package com.memory_atelier.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CreditServiceTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .provider(Provider.LOCAL)
                .email("credit-unit-test@example.com")
                .nickname("크레딧유닛테스트")
                .password("dummy")
                .build();
        user.grantCredit(100);
        // deduct()는 영속성 컨텍스트를 거치지 않는 벌크 UPDATE라, DB에 실제로 반영돼 있어야 한다.
        userId = userRepository.saveAndFlush(user).getUserId();
    }

    @Test
    void 잔액만큼_차감하면_성공한다() {
        creditService.deduct(userId, 100);

        assertThat(userRepository.findById(userId).orElseThrow().getCredit()).isZero();
    }

    @Test
    void 잔액보다_많이_차감하려_하면_거절된다() {
        assertThatThrownBy(() -> creditService.deduct(userId, 101))
                .isInstanceOf(CustomException.class);

        // 실패한 차감은 잔액에 영향을 주지 않는다.
        assertThat(userRepository.findById(userId).orElseThrow().getCredit()).isEqualTo(100);
    }

    @Test
    void amount가_0_이하면_차감도_지급도_거절된다() {
        assertThatThrownBy(() -> creditService.deduct(userId, 0)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> creditService.grant(userId, -5)).isInstanceOf(CustomException.class);
    }

    @Test
    void 관리자_지급은_잔액을_늘린다() {
        User granted = creditService.grant(userId, 50);

        assertThat(granted.getCredit()).isEqualTo(150);
    }
}
