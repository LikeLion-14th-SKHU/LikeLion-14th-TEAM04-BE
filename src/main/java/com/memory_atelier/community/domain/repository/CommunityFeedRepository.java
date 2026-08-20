package com.memory_atelier.community.domain.repository;

import com.memory_atelier.certificate.domain.Certificate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// 공개 에디션 피드 조회 전용
// {@code Certificate}를 앵커로 삼는다 — 확정된(보증서가 발급된) 콘셉트만 카드가 될 수 있고,
// 보증서에 에디션명·카테고리 스냅샷이 이미 있어 추가 조인 없이 바로 쓸 수 있기 때문이다

// {@code Certificate}/{@code EditionConcept}가 아니라 여기(community)에 두는 이유는,
// 목록을 목록으로 만드는 조건(공개 여부·좋아요 수)이 전부 community 소유라서다 — edition/certificate 쪽에 두면 그 도메인들이 {@code PublicSetting}과 {@code Like}를 알아야 해서 의존이 순환한다
// 지금 방향은 community → certificate/edition/publicsetting 한쪽뿐이다
// @link Repository}를 직접 구현해 CRUD를 노출하지 않는다 — 여기는 피드 읽기 전용이고, 보증서를 저장·삭제하는 책임은 certificate에 있다

// 공개 조건
// 세 가지를 모두 쿼리에서 처리한다. 메모리로 가져와 걸러내면 페이지에 담기는 건수가 요청한 size보다 적어지고 총 개수도 틀어진다
// 공개 여부 — 카드 단위 설정이 있으면 그 값이 컬렉션 전체 설정을 덮고, 둘 다 없으면 비공개다
// {@code coalesce}가 이 우선순위를 그대로 표현한다.

// 확정 여부 — {@code Certificate}에서 시작하므로 확정 전(보증서 없음) 콘셉트는 애초에 후보에 오르지 않는다
// 검색 — 에디션명과 닉네임을 함께 훑는다

// id를 먼저 뽑는 이유
// 인기순은 좋아요를 join해 집계해야 하는데, 그 상태로 연관까지 fetch하면 행이 뻥튀기돼 페이징이 깨진다
// 그래서 id 페이지를 먼저 확정하고 내용은 {@link #findAllWithOwnerByIdIn}으로 다시 읽는다
public interface CommunityFeedRepository extends Repository<Certificate, Long> {

    // 인기순(좋아요 많은 순). 같으면 최신 순으로 갈린다
    @Query("""
            select cert.certificateId
            from Certificate cert
              join cert.concept c
              join c.generation g
              join g.memory m
              join m.user u
              left join PublicSetting card on card.user = u and card.scopeKey = c.conceptId
              left join PublicSetting col on col.user = u and col.scopeKey = 0L
              left join Like l on l.concept = c
            where coalesce(card.isPublic, col.isPublic, false) = true
              and (:keyword = ''
                   or lower(cert.editionName) like lower(concat('%', :keyword, '%'))
                   or lower(u.nickname) like lower(concat('%', :keyword, '%')))
            group by cert.certificateId
            order by count(l) desc, cert.certificateId desc
            """)
    List<Long> findPopularCertificateIds(@Param("keyword") String keyword, Pageable pageable);

    // 최신순. 좋아요를 집계할 필요가 없어 join도 group by도 걸지 않는다.
    @Query("""
            select cert.certificateId
            from Certificate cert
              join cert.concept c
              join c.generation g
              join g.memory m
              join m.user u
              left join PublicSetting card on card.user = u and card.scopeKey = c.conceptId
              left join PublicSetting col on col.user = u and col.scopeKey = 0L
            where coalesce(card.isPublic, col.isPublic, false) = true
              and (:keyword = ''
                   or lower(cert.editionName) like lower(concat('%', :keyword, '%'))
                   or lower(u.nickname) like lower(concat('%', :keyword, '%')))
            order by cert.certificateId desc
            """)
    List<Long> findLatestCertificateIds(@Param("keyword") String keyword, Pageable pageable);

    // 위 조건에 걸리는 전체 건수. 페이지 메타를 만들 때 쓴다
    @Query("""
            select count(distinct cert.certificateId)
            from Certificate cert
              join cert.concept c
              join c.generation g
              join g.memory m
              join m.user u
              left join PublicSetting card on card.user = u and card.scopeKey = c.conceptId
              left join PublicSetting col on col.user = u and col.scopeKey = 0L
            where coalesce(card.isPublic, col.isPublic, false) = true
              and (:keyword = ''
                   or lower(cert.editionName) like lower(concat('%', :keyword, '%'))
                   or lower(u.nickname) like lower(concat('%', :keyword, '%')))
            """)
    long countVisibleCertificates(@Param("keyword") String keyword);

    // 피드 카드에 실을 보증서를 소유자까지 한 번에 읽는다. 정렬은 보장하지 않으므로 호출자가 id 순서대로 다시 늘어놓는다
    @Query("""
            select cert from Certificate cert
              join fetch cert.concept c
              join fetch c.generation g
              join fetch g.memory m
              join fetch m.user
            where cert.certificateId in :certificateIds
            """)
    List<Certificate> findAllWithOwnerByIdIn(@Param("certificateIds") List<Long> certificateIds);

    // 카드 한 장. 공개 여부는 호출자가 따로 판단한다
    @Query("""
            select cert from Certificate cert
              join fetch cert.concept c
              join fetch c.generation g
              join fetch g.memory m
              join fetch m.user
            where c.conceptId = :conceptId
            """)
    Optional<Certificate> findWithOwnerByConceptId(@Param("conceptId") Long conceptId);
}
