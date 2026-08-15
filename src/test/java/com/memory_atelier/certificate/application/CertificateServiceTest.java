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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CertificateServiceTest {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private EditionGenerationRepository generationRepository;

    @Autowired
    private EditionConceptRepository conceptRepository;

    private Long ownerId;
    private Long otherUserId;
    private EditionConcept finalConcept;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .provider(Provider.LOCAL)
                .email("certificate-unit-owner@example.com")
                .nickname("보증서유닛오너")
                .password("dummy")
                .build();
        ownerId = userRepository.saveAndFlush(owner).getUserId();

        User other = User.builder()
                .provider(Provider.LOCAL)
                .email("certificate-unit-other@example.com")
                .nickname("보증서유닛남")
                .password("dummy")
                .build();
        otherUserId = userRepository.saveAndFlush(other).getUserId();

        Memory memory = memoryRepository.save(Memory.builder()
                .user(owner)
                .photoUrl("https://example.com/photo.png")
                .categoryMain("가방")
                .categorySub("백팩")
                .story("테스트 사연")
                .build());

        EditionGeneration generation = generationRepository.save(EditionGeneration.builder()
                .memory(memory)
                .generationNo(1)
                .storySnapshot("테스트 사연")
                .build());
        generation.applyNarrative(List.of("Heritage Edition"), "보증서 문구");

        finalConcept = conceptRepository.save(EditionConcept.builder()
                .generation(generation)
                .displayOrder(1)
                .build());
        finalConcept.applyConceptImage(
                "가방", null, "컨셉명", "safe", 1, "베이스", "방식", null, "https://example.com/concept.png", 80);
        finalConcept.unlock();
        finalConcept.markFinal();
    }

    @Test
    void 확정된_콘셉트에_보증서를_발급하면_발급시점_값이_스냅샷으로_저장된다() {
        Certificate certificate = certificateService.issue(finalConcept);

        assertThat(certificate.getEditionName()).isEqualTo("Heritage Edition");
        assertThat(certificate.getCategory()).isEqualTo("가방");
        assertThat(certificate.getStorySnapshot()).isEqualTo("테스트 사연");
        assertThat(certificate.getCertificateText()).isEqualTo("보증서 문구");
        assertThat(certificate.getEditionNumber()).startsWith("B");
    }

    @Test
    void 같은_생성_배치에서_두번째_발급을_시도하면_거절된다() {
        certificateService.issue(finalConcept);

        assertThatThrownBy(() -> certificateService.issue(finalConcept))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 보증서_발급후_에디션명을_바꿔도_이미_발급된_보증서는_바뀌지_않는다() {
        Certificate certificate = certificateService.issue(finalConcept);
        finalConcept.getGeneration().selectEditionName("바뀐 이름");

        Certificate reloaded = certificateService.getOwnedCertificate(ownerId, finalConcept.getConceptId());
        assertThat(reloaded.getEditionName()).isEqualTo("Heritage Edition");
        assertThat(certificate.getEditionName()).isEqualTo("Heritage Edition");
    }

    @Test
    void 다른_사용자는_보증서를_조회할_수_없다() {
        certificateService.issue(finalConcept);

        assertThatThrownBy(() -> certificateService.getOwnedCertificate(otherUserId, finalConcept.getConceptId()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 발급된_보증서는_내_컬렉션_목록에_보인다() {
        certificateService.issue(finalConcept);

        assertThat(certificateService.getMyCertificates(ownerId, PageRequest.of(0, 20)).getContent())
                .hasSize(1);
    }
}
