package com.memory_atelier.certificate.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memory_atelier.certificate.domain.Certificate;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.edition.domain.repository.EditionGenerationRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.memory.domain.Memory;
import com.memory_atelier.memory.domain.repository.MemoryRepository;
import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CertificateIssueFacadeTest {

    @Autowired
    private CertificateIssueFacade certificateIssueFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private EditionGenerationRepository generationRepository;

    @Autowired
    private EditionConceptRepository conceptRepository;

    private Long ownerId;
    private EditionGeneration generation;
    private EditionConcept first;
    private EditionConcept second;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .provider(Provider.LOCAL)
                .email("certificate-facade-owner@example.com")
                .nickname("보증서파사드")
                .password("dummy")
                .build();
        ownerId = userRepository.saveAndFlush(owner).getUserId();

        Memory memory = memoryRepository.save(Memory.builder()
                .user(owner)
                .photoUrl("https://example.com/photo.png")
                .categoryMain("가방")
                .categorySub("백팩")
                .story("테스트 사연")
                .build());

        generation = generationRepository.save(EditionGeneration.builder()
                .memory(memory)
                .generationNo(1)
                .storySnapshot("테스트 사연")
                .targetCategoryMain("가방")
                .targetCategorySub("토트백")
                .build());
        generation.applyNarrative(List.of("Heritage Edition"), "보증서 문구");

        first = conceptRepository.save(EditionConcept.builder().generation(generation).displayOrder(1).build());
        first.applyConceptImage(
                "가방", null, "컨셉명1", "safe", 1, "베이스1", "방식1", null, "https://example.com/concept1.png", 80);
        first.unlock();

        second = conceptRepository.save(EditionConcept.builder().generation(generation).displayOrder(2).build());
        second.applyConceptImage(
                "가방", null, "컨셉명2", "bold", 3, "베이스2", "방식2", null, "https://example.com/concept2.png", 90);
        second.unlock();
    }

    @Test
    void 확정과_발급이_한번에_성공한다() {
        Certificate certificate = certificateIssueFacade.selectFinal(ownerId, first.getConceptId());

        assertThat(certificate.getConcept().getConceptId()).isEqualTo(first.getConceptId());
        assertThat(conceptRepository.findById(first.getConceptId()).orElseThrow().isFinal()).isTrue();
    }

    @Test
    void 같은_배치의_다른_콘셉트를_또_확정하면_거절되고_먼저_확정된_카드는_그대로다() {
        certificateIssueFacade.selectFinal(ownerId, first.getConceptId());

        assertThatThrownBy(() -> certificateIssueFacade.selectFinal(ownerId, second.getConceptId()))
                .isInstanceOf(CustomException.class);
    }
}
