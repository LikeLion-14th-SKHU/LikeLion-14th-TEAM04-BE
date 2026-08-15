package com.memory_atelier.certificate.domain.repository;

import com.memory_atelier.certificate.domain.Certificate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    // 같은 생성 배치에서 보증서가 이미 나갔는지 확인한다(보증서는 발급 후 불변)
    // 경쟁 상태에서는 이 확인만으로 부족해 유니크 제약이 최종 방어선이 된다
    boolean existsByGenerationGenerationId(Long generationId);

    // 소유권 확인은 보증서 → 콘셉트 → 배치 → 추억 → 사용자를 타고 올라가므로 함께 가져온다
    @Query("""
            select c from Certificate c
              join fetch c.concept concept
              join fetch concept.generation g
              join fetch g.memory m
              join fetch m.user
            where concept.conceptId = :conceptId
            """)
    Optional<Certificate> findByConceptId(@Param("conceptId") Long conceptId);

    // 내 컬렉션 목록. 카드 앞면에 콘셉트 이미지가 필요하므로 함께 가져온다
    @Query(value = """
            select c from Certificate c
              join fetch c.concept concept
              join fetch concept.generation g
              join fetch g.memory m
            where m.user.userId = :userId
            order by c.createdAt desc, c.certificateId desc
            """,
            countQuery = """
            select count(c) from Certificate c
            where c.concept.generation.memory.user.userId = :userId
            """)
    Page<Certificate> findAllByOwnerIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
