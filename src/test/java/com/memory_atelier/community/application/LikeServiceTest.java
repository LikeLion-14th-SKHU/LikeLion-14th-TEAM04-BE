package com.memory_atelier.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// PublicSettingCreator가 REQUIRES_NEW로 행을 만들기 때문에 @Transactional 롤백 방식은 못 쓴다
// (#18에서 확인) — 실제로 커밋하고 tearDown에서 직접 지운다
@SpringBootTest
class LikeServiceTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private PublicSettingService publicSettingService;

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
    private Long otherUserId;
    private Long conceptId;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .provider(Provider.LOCAL)
                .email("like-unit-owner@example.com")
                .nickname("좋아요오너")
                .password("dummy")
                .build();
        ownerId = userRepository.saveAndFlush(owner).getUserId();

        User other = User.builder()
                .provider(Provider.LOCAL)
                .email("like-unit-other@example.com")
                .nickname("좋아요남")
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
        EditionConcept concept = conceptRepository.save(
                EditionConcept.builder().generation(generation).displayOrder(1).build());
        concept.unlock();
        conceptId = conceptRepository.saveAndFlush(concept).getConceptId();
    }

    @AfterEach
    void tearDown() {
        likeRepository.deleteAll();
        publicSettingRepository.deleteAll();
        conceptRepository.deleteAll();
        generationRepository.deleteAll();
        memoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 소유자는_비공개_콘셉트에도_좋아요를_누를_수_있다() {
        likeService.like(ownerId, conceptId);

        assertThat(likeService.countLikes(ownerId, conceptId)).isEqualTo(1);
        assertThat(likeService.isLikedByUser(ownerId, conceptId)).isTrue();
    }

    @Test
    void 공개되지_않은_콘셉트에_타인이_좋아요를_시도하면_거절된다() {
        assertThatThrownBy(() -> likeService.like(otherUserId, conceptId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 공개된_콘셉트는_타인도_좋아요를_누를_수_있다() {
        publicSettingService.toggleCard(ownerId, conceptId, true);

        likeService.like(otherUserId, conceptId);

        assertThat(likeService.countLikes(null, conceptId)).isEqualTo(1);
    }

    @Test
    void 같은_콘셉트에_두번_좋아요를_누르면_거절된다() {
        likeService.like(ownerId, conceptId);

        assertThatThrownBy(() -> likeService.like(ownerId, conceptId))
                .isInstanceOf(CustomException.class);
        assertThat(likeService.countLikes(ownerId, conceptId)).isEqualTo(1);
    }

    @Test
    void 좋아요를_취소하면_카운트가_줄어든다() {
        likeService.like(ownerId, conceptId);

        likeService.unlike(ownerId, conceptId);

        assertThat(likeService.countLikes(ownerId, conceptId)).isZero();
    }

    @Test
    void 비로그인_사용자는_공개된_콘셉트의_좋아요_수는_보되_눌렀는지는_항상_false다() {
        publicSettingService.toggleCard(ownerId, conceptId, true);
        likeService.like(ownerId, conceptId);

        assertThat(likeService.countLikes(null, conceptId)).isEqualTo(1);
        assertThat(likeService.isLikedByUser(null, conceptId)).isFalse();
    }
}
