package com.memory_atelier.recommend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memory_atelier.edition.application.PipelineEvents;
import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.edition.domain.repository.EditionGenerationRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.memory.domain.Memory;
import com.memory_atelier.memory.domain.repository.MemoryRepository;
import com.memory_atelier.recommend.domain.Recommendation;
import com.memory_atelier.recommend.domain.repository.RecommendationRepository;
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
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationRepository recommendationRepository;

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

    private static final List<PipelineEvents.CurationReceived.RecommendationPayload> PAYLOAD = List.of(
            new PipelineEvents.CurationReceived.RecommendationPayload(
                    "p-1", "상품1", "이유1", "태그라인1", "https://example.com/p1.png"),
            new PipelineEvents.CurationReceived.RecommendationPayload(
                    "p-2", "상품2", "이유2", "태그라인2", null));

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .provider(Provider.LOCAL)
                .email("recommend-unit-owner@example.com")
                .nickname("추천유닛오너")
                .password("dummy")
                .build();
        ownerId = userRepository.saveAndFlush(owner).getUserId();

        User other = User.builder()
                .provider(Provider.LOCAL)
                .email("recommend-unit-other@example.com")
                .nickname("추천유닛남")
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

        EditionConcept concept = conceptRepository.save(EditionConcept.builder()
                .generation(generation)
                .displayOrder(1)
                .build());
        conceptId = concept.getConceptId();
    }

    @Test
    void 큐레이션_결과를_저장하면_추천_목록으로_조회된다() {
        recommendationService.saveFromCuration(conceptId, PAYLOAD);

        Recommendation recommendation = recommendationService.getOwnedRecommendation(ownerId, conceptId);
        assertThat(recommendation.getItems()).hasSize(2);
        assertThat(recommendation.getItems().get(0).productId()).isEqualTo("p-1");
        assertThat(recommendation.getItems().get(1).imageUrl()).isNull();
    }

    @Test
    void 같은_콘셉트에_이벤트가_중복으로_와도_한_번만_저장된다() {
        recommendationService.saveFromCuration(conceptId, PAYLOAD);
        recommendationService.saveFromCuration(conceptId, PAYLOAD);

        assertThat(recommendationRepository.findByConceptId(conceptId).orElseThrow().getItems()).hasSize(2);
    }

    @Test
    void 아직_큐레이션이_안_끝난_콘셉트를_조회하면_거절된다() {
        assertThatThrownBy(() -> recommendationService.getOwnedRecommendation(ownerId, conceptId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 다른_사용자의_추천은_존재하지_않는_것처럼_거절된다() {
        recommendationService.saveFromCuration(conceptId, PAYLOAD);

        assertThatThrownBy(() -> recommendationService.getOwnedRecommendation(otherUserId, conceptId))
                .isInstanceOf(CustomException.class);
    }
}
