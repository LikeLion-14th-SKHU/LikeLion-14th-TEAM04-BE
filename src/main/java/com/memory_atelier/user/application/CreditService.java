package com.memory_atelier.user.application;

import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 크레딧 잔액의 차감·지급 진입점
// 다른 도메인은 이 클래스만 호출하고 User의 credit 컬럼에 직접 접근하지 않는다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditService {

    private final UserRepository userRepository;
    private final UserService userService;

    // 잔액에서 차감한다
    // 잔액이 모자라면 {@link ErrorCode#INSUFFICIENT_CREDIT}로 거절한다
    // 차감은 호출자의 트랜잭션에 참여하므로, 크레딧을 쓴 작업이 뒤에서 실패하면 차감도 함께 롤백된다
    @Transactional
    public void deduct(Long userId, int amount) {
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_CREDIT_AMOUNT);
        }
        if (userRepository.deductCredit(userId, amount) == 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_CREDIT);
        }
    }

    // 관리자 지급
    @Transactional
    public User grant(Long userId, int amount) {
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_CREDIT_AMOUNT);
        }
        User user = userService.getActiveUser(userId);
        user.grantCredit(amount);
        return user;
    }
}
