package com.memory_atelier.user.domain.repository;

import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 회원 조회 (로그인 시 사용)
    Optional<User> findByEmailAndProvider(String email, Provider provider);

    // 쿨다운이 끝난 뒤 같은 이메일로 재가입할 때, 그 자리를 차지하고 있는 옛 탈퇴 계정을 찾는다.
    // email에 유니크 제약이 있어서 존재한다면 최대 1건이다
    Optional<User> findByEmail(String email);

    // 회원가입 시 이메일 차단 여부 확인. 활성 유저가 있거나, 탈퇴한 유저라도 유예+쿨다운
    // 기간(cooldownCutoff 이후에 탈퇴한 경우)이 안 지났으면 막는다 — WithdrawalPolicy.emailBlockCutoff() 참고
    @Query("select count(u) > 0 from User u "
            + "where u.email = :email and (u.deletedAt is null or u.deletedAt > :cooldownCutoff)")
    boolean existsBlockingEmail(@Param("email") String email, @Param("cooldownCutoff") LocalDateTime cooldownCutoff);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    // 닉네임으로 회원 검색 (부분 일치, 탈퇴 회원 제외)
    Page<User> findByNicknameContainingAndDeletedAtIsNull(String nickname, Pageable pageable);

    //크레딧 원자적 차감
    // 조회 후 자바에서 빼는 방식은 동시 요청 두 건이 같은 잔액을 읽어 둘 다 통과할 수 있어서, 이 조건부 UPDATE 한 문장으로 처리한다
    // 영향받은 row가 0이면 잔액 부족이거나 탈퇴한 계정이라는 뜻
    @Modifying(clearAutomatically = true)
    @Query("update User u set u.credit = u.credit - :amount "
            + "where u.userId = :userId and u.credit >= :amount and u.deletedAt is null")
    int deductCredit(@Param("userId") Long userId, @Param("amount") int amount);
}