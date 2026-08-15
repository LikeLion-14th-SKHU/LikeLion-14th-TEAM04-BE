package com.memory_atelier.publicsetting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.edition.domain.repository.EditionGenerationRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.memory.domain.Memory;
import com.memory_atelier.memory.domain.repository.MemoryRepository;
import com.memory_atelier.publicsetting.domain.PublicSetting;
import com.memory_atelier.publicsetting.domain.PublicSettingTargetType;
import com.memory_atelier.publicsetting.domain.repository.PublicSettingRepository;
import com.memory_atelier.user.domain.Provider;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// PublicSettingCreator가 REQUIRES_NEW(별도 커넥션)로 행을 만들기 때문에, 테스트 전체를
// @Transactional로 감싸 롤백하는 방식은 못 쓴다 — REQUIRES_NEW 쪽에서는 아직 커밋되지 않은
// setUp()의 User/EditionConcept가 안 보여 FK 위반이 난다. 그래서 각 커밋을 실제로 반영하고
// tearDown에서 직접 지운다
@SpringBootTest
class PublicSettingServiceTest {

    @Autowired
    private PublicSettingService publicSettingService;

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
    private Long otherUserId;
    private Long conceptId;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .provider(Provider.LOCAL)
                .email("public-setting-owner@example.com")
                .nickname("공개설정오너")
                .password("dummy")
                .build();
        ownerId = userRepository.saveAndFlush(owner).getUserId();

        User other = User.builder()
                .provider(Provider.LOCAL)
                .email("public-setting-other@example.com")
                .nickname("공개설정남")
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
        conceptId = conceptRepository.save(EditionConcept.builder().generation(generation).displayOrder(1).build())
                .getConceptId();
    }

    @AfterEach
    void tearDown() {
        publicSettingRepository.deleteAll();
        conceptRepository.deleteAll();
        generationRepository.deleteAll();
        memoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 한번도_토글하지_않아도_컬렉션_설정을_조회하면_비공개_기본값과_공유토큰이_생성된다() {
        PublicSettingService.MySettings settings = publicSettingService.getMySettings(ownerId);

        assertThat(settings.collection().isPublic()).isFalse();
        assertThat(settings.collection().getShareToken()).isNotBlank();
        assertThat(settings.cards()).isEmpty();
    }

    @Test
    void 컬렉션을_공개로_토글하면_반영된다() {
        publicSettingService.toggleCollection(ownerId, true);

        assertThat(publicSettingService.getMySettings(ownerId).collection().isPublic()).isTrue();
    }

    @Test
    void 같은_스코프를_두번_조회해도_설정_행은_하나만_생긴다() {
        publicSettingService.getMySettings(ownerId);
        publicSettingService.getMySettings(ownerId);

        assertThat(publicSettingRepository.findAllByUserUserIdAndTargetType(ownerId, PublicSettingTargetType.ALL_COLLECTION))
                .hasSize(1);
    }

    @Test
    void 카드_단위로_공개설정하면_카드_목록에만_나타난다() {
        publicSettingService.toggleCard(ownerId, conceptId, true);

        PublicSettingService.MySettings settings = publicSettingService.getMySettings(ownerId);
        assertThat(settings.cards()).hasSize(1);
        assertThat(settings.cards().get(0).getScopeKey()).isEqualTo(conceptId);
        assertThat(settings.cards().get(0).isPublic()).isTrue();
        assertThat(settings.collection().isPublic()).isFalse();
    }

    @Test
    void 남의_콘셉트를_카드_단위로_설정하려하면_거절된다() {
        assertThatThrownBy(() -> publicSettingService.toggleCard(otherUserId, conceptId, true))
                .isInstanceOf(CustomException.class);
    }
}
