package com.memory_atelier.user.domain.repository;

import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 회원 조회 (로그인 시 사용)
    Optional<User> findByEmailAndProvider(String email, Provider provider);

    // 이메일 중복 체크 (회원가입 시 사용)
    boolean existsByEmail(String email);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);
}