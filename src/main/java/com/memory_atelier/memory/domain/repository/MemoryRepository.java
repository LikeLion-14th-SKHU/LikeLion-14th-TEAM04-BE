package com.memory_atelier.memory.domain.repository;

import com.memory_atelier.memory.domain.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    Page<Memory> findAllByUserUserIdOrderByMemoryIdDesc(Long userId, Pageable pageable);
}
