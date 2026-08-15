package com.memory_atelier.certificate.application;

import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.edition.application.EditionConceptService;
import com.memory_atelier.edition.domain.EditionConcept;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 최종 확정과 보증서 발급을 한 트랜잭션에서 잇는 진입점.
//
// certificate가 edition을 참조하는 건 불가피하다(보증서가 콘셉트·배치를 가리켜야 하니까).
// 그런데 발급을 EditionConceptService 안에서 하면 반대 방향 의존(edition → certificate)까지
// 생겨 두 패키지가 서로를 참조하게 된다. 그래서 반대 방향 의존만 이 클래스로 모은다 —
// edition은 보증서를 전혀 모른 채 "확정 표시"까지만 하고, 여기서 이어받아 발급한다.
//
// 두 단계가 한 트랜잭션이어야 하는 이유는 둘 다다. 확정만 되고 보증서가 없으면 컬렉션에
// 뒷면 없는 카드가 생기고, 보증서만 있고 확정이 안 되면 어느 후보의 것인지 알 수 없다.
@Service
@RequiredArgsConstructor
public class CertificateIssueFacade {

    private final EditionConceptService editionConceptService;
    private final CertificateService certificateService;

    // 확정 범위는 생성 배치 단위다. 같은 배치의 다른 후보를 동시에 확정하려 하면
    // certificates.generation_id 유니크 제약이 한쪽만 통과시킨다
    @Transactional
    public Certificate selectFinal(Long userId, Long conceptId) {
        EditionConcept concept = editionConceptService.markFinal(userId, conceptId);
        return certificateService.issue(concept);
    }
}
