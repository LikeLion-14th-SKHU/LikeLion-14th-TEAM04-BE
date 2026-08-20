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
class CommunityFeedServiceTest {

    @Autowired
    private CommunityFeedService communityFeedService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private PublicSettingService publicSettingService;

    @Autowired
    private LikeService likeService;

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

    private Long collectionPublicOwnerId;
    private Long cardPublicOwnerId;
    private Long privateOwnerId;
    private Long collectionPublicConceptId;
    private Long cardPublicConceptId;
    private Long privateConceptId;

    @BeforeEach
    void setUp() {
        collectionPublicOwnerId = newUser("feed-collection-public@example.com", "컬렉션공개");
        cardPublicOwnerId = newUser("feed-card-public@example.com", "카드공개");
        privateOwnerId = newUser("feed-private@example.com", "비공개");

        collectionPublicConceptId = newFinalizedConcept(collectionPublicOwnerId, "컬렉션공개", "Collection Public Edition");
        cardPublicConceptId = newFinalizedConcept(cardPublicOwnerId, "카드공개", "Card Public Edition");
        privateConceptId = newFinalizedConcept(privateOwnerId, "비공개", "Private Edition");

        publicSettingService.toggleCollection(collectionPublicOwnerId, true);
        publicSettingService.toggleCard(cardPublicOwnerId, cardPublicConceptId, true);
        // privateOwnerId는 아무 설정도 건드리지 않는다 — 기본 비공개
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
    void 컬렉션_전체_공개와_카드_단위_공개_둘_다_피드에_나타나고_비공개는_안나타난다() {
        List<Long> feedConceptIds = communityFeedService.getFeed(null, CommunityFeedService.SortBy.LATEST, PageRequest.of(0, 20))
                .getContent().stream()
                .map(card -> card.certificate().getConcept().getConceptId())
                .toList();

        assertThat(feedConceptIds).contains(collectionPublicConceptId, cardPublicConceptId);
        assertThat(feedConceptIds).doesNotContain(privateConceptId);
    }

    @Test
    void 인기순은_좋아요가_많은_순으로_정렬된다() {
        likeService.like(privateOwnerId, cardPublicConceptId);
        likeService.like(cardPublicOwnerId, cardPublicConceptId);

        List<Long> popular = communityFeedService.getFeed(null, CommunityFeedService.SortBy.POPULAR, PageRequest.of(0, 20))
                .getContent().stream()
                .map(card -> card.certificate().getConcept().getConceptId())
                .toList();

        assertThat(popular.getFirst()).isEqualTo(cardPublicConceptId);
    }

    @Test
    void 에디션명으로_검색된다() {
        var result = communityFeedService.getFeed("Collection Public", CommunityFeedService.SortBy.LATEST, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().certificate().getConcept().getConceptId())
                .isEqualTo(collectionPublicConceptId);
    }

    @Test
    void 닉네임으로_검색된다() {
        var result = communityFeedService.getFeed("카드공개", CommunityFeedService.SortBy.LATEST, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().certificate().getConcept().getConceptId())
                .isEqualTo(cardPublicConceptId);
    }

    @Test
    void 비공개_카드_상세를_조회하면_거절된다() {
        assertThatThrownBy(() -> communityFeedService.getCard(privateConceptId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 공개_카드_상세는_조회된다() {
        var card = communityFeedService.getCard(collectionPublicConceptId);

        assertThat(card.certificate().getEditionName()).isEqualTo("Collection Public Edition");
    }

    private Long newUser(String email, String nickname) {
        return userRepository.saveAndFlush(User.builder()
                .provider(Provider.LOCAL)
                .email(email)
                .nickname(nickname)
                .password("dummy")
                .build()).getUserId();
    }

    private Long newFinalizedConcept(Long ownerId, String story, String editionName) {
        User owner = userRepository.findById(ownerId).orElseThrow();
        Memory memory = memoryRepository.save(Memory.builder()
                .user(owner)
                .photoUrl("https://example.com/photo.png")
                .categoryMain("가방")
                .categorySub("백팩")
                .story(story)
                .build());
        EditionGeneration generation = generationRepository.save(EditionGeneration.builder()
                .memory(memory)
                .generationNo(1)
                .storySnapshot(story)
                .targetCategoryMain("가방")
                .targetCategorySub("토트백")
                .build());
        generation.applyNarrative(List.of(editionName), "보증서 문구");
        generationRepository.saveAndFlush(generation);

        EditionConcept concept = conceptRepository.save(
                EditionConcept.builder().generation(generation).displayOrder(1).build());
        concept.applyConceptImage(
                "가방", null, "컨셉명", "safe", 1, "베이스", "방식", null, "https://example.com/concept.png", 80);
        concept.unlock();
        concept.markFinal();
        conceptRepository.saveAndFlush(concept);

        certificateService.issue(concept);
        return concept.getConceptId();
    }
}
