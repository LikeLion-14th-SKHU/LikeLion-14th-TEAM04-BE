package com.memory_atelier.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memory_atelier.certificate.application.CertificateService;
import com.memory_atelier.certificate.domain.repository.CertificateRepository;
import com.memory_atelier.community.domain.repository.LikeRepository;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.edition.domain.repository.EditionGenerationRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.memory.domain.Memory;
import com.memory_atelier.memory.domain.repository.MemoryRepository;
import com.memory_atelier.publicsetting.application.PublicSettingService;
import com.memory_atelier.publicsetting.domain.PublicSetting;
import com.memory_atelier.publicsetting.domain.repository.PublicSettingRepository;
import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

// PublicSettingCreator가 REQUIRES_NEW로 행을 만들기 때문에 @Transactional 롤백 방식은 못 쓴다(#18)
@SpringBootTest
class SharedViewServiceTest {

    @Autowired
    private SharedViewService sharedViewService;

    @Autowired
    private PublicSettingService publicSettingService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private PublicSettingRepository publicSettingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private EditionGenerationRepository generationRepository;

    @Autowired
    private EditionConceptRepository conceptRepository;

    private Long ownerId;
    private Long firstConceptId;
    private Long secondConceptId;

    @BeforeEach
    void setUp() {
        ownerId = userRepository.saveAndFlush(User.builder()
                .provider(Provider.LOCAL)
                .email("shared-view-owner@example.com")
                .nickname("공유뷰오너")
                .password("dummy")
                .build()).getUserId();

        User owner = userRepository.findById(ownerId).orElseThrow();
        Memory memory = memoryRepository.save(Memory.builder()
                .user(owner)
                .photoUrl("https://example.com/photo.png")
                .categoryMain("가방")
                .categorySub("백팩")
                .story("테스트 사연")
                .build());

        // 보증서는 생성 배치(EditionGeneration)당 하나만 발급되므로, 카드 두 장을 모두 확정하려면
        // 배치를 따로 둬야 한다
        firstConceptId = finalizeConcept(newGeneration(memory, 1), 1);
        secondConceptId = finalizeConcept(newGeneration(memory, 2), 1);
    }

    private EditionGeneration newGeneration(Memory memory, int generationNo) {
        EditionGeneration generation = generationRepository.save(EditionGeneration.builder()
                .memory(memory)
                .generationNo(generationNo)
                .storySnapshot("테스트 사연")
                .build());
        generation.applyNarrative(List.of("Shared Edition " + generationNo), "보증서 문구");
        return generationRepository.saveAndFlush(generation);
    }

    @AfterEach
    void tearDown() {
        likeRepository.deleteAll();
        publicSettingRepository.deleteAll();
        certificateRepository.deleteAll();
        conceptRepository.deleteAll();
        generationRepository.deleteAll();
        memoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 카드_단위_공유는_그_카드_한장만_보여준다() {
        publicSettingService.toggleCard(ownerId, firstConceptId, true);
        PublicSetting setting = publicSettingService.getMySettings(ownerId).cards().getFirst();

        var page = sharedViewService.getSharedCards(setting, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().certificate().getConcept().getConceptId()).isEqualTo(firstConceptId);
    }

    @Test
    void 컬렉션_전체_공유는_카드_단위로_비공개한_카드를_제외한다() {
        publicSettingService.toggleCollection(ownerId, true);
        publicSettingService.toggleCard(ownerId, secondConceptId, false);
        PublicSetting setting = publicSettingService.getMySettings(ownerId).collection();

        var page = sharedViewService.getSharedCards(setting, PageRequest.of(0, 10));

        List<Long> conceptIds = page.getContent().stream()
                .map(card -> card.certificate().getConcept().getConceptId())
                .toList();
        assertThat(conceptIds).contains(firstConceptId);
        assertThat(conceptIds).doesNotContain(secondConceptId);
    }

    @Test
    void 비공개_토큰으로는_설정을_가져올_수_없다() {
        publicSettingService.getMySettings(ownerId); // 컬렉션 설정을 기본(비공개)으로 생성
        PublicSetting setting = publicSettingService.getMySettings(ownerId).collection();

        assertThatThrownBy(() -> sharedViewService.getSetting(setting.getShareToken()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 존재하지_않는_토큰은_거절된다() {
        assertThatThrownBy(() -> sharedViewService.getSetting("no-such-token"))
                .isInstanceOf(CustomException.class);
    }

    private Long finalizeConcept(EditionGeneration generation, int displayOrder) {
        EditionConcept concept = conceptRepository.save(
                EditionConcept.builder().generation(generation).displayOrder(displayOrder).build());
        concept.applyConceptImage(
                "가방", null, "컨셉명" + displayOrder, "safe", 1, "베이스", "방식", null,
                "https://example.com/concept" + displayOrder + ".png", 80);
        concept.unlock();
        concept.markFinal();
        conceptRepository.saveAndFlush(concept);
        certificateService.issue(concept);
        return concept.getConceptId();
    }
}
