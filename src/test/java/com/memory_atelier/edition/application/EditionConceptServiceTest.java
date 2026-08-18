package com.memory_atelier.edition.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memory_atelier.edition.domain.EditionConcept;
import com.memory_atelier.edition.domain.EditionGeneration;
import com.memory_atelier.edition.domain.repository.EditionConceptRepository;
import com.memory_atelier.edition.domain.repository.EditionGenerationRepository;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.memory.domain.Memory;
import com.memory_atelier.memory.domain.repository.MemoryRepository;
import com.memory_atelier.user.application.CreditPolicy;
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
class EditionConceptServiceTest {

    @Autowired
    private EditionConceptService editionConceptService;

    @Autowired
    private CreditPolicy creditPolicy;

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
    private Long readyConceptId;
    private Long pendingConceptId;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .provider(Provider.LOCAL)
                .email("concept-unit-owner@example.com")
                .nickname("콘셉트유닛오너")
                .password("dummy")
                .build();
        owner.grantCredit(100);
        ownerId = userRepository.saveAndFlush(owner).getUserId();

        User other = User.builder()
                .provider(Provider.LOCAL)
                .email("concept-unit-other@example.com")
                .nickname("콘셉트유닛남")
                .password("dummy")
                .build();
        otherUserId = userRepository.saveAndFlush(other).getUserId();

        EditionGeneration generation = generationRepository.save(EditionGeneration.builder()
                .memory(newMemory(owner))
                .generationNo(1)
                .storySnapshot("테스트 사연")
                .targetCategoryMain("가방")
                .targetCategorySub("토트백")
                .build());

        EditionConcept readyConcept = conceptRepository.save(EditionConcept.builder()
                .generation(generation)
                .displayOrder(1)
                .build());
        readyConcept.applyConceptImage(
                "가방", null, "컨셉명", "safe", 1, "베이스", "방식", null, "https://example.com/concept.png", 80);
        readyConceptId = readyConcept.getConceptId();

        EditionConcept pendingConcept = conceptRepository.save(EditionConcept.builder()
                .generation(generation)
                .displayOrder(2)
                .build());
        pendingConceptId = pendingConcept.getConceptId();
    }

    private Memory newMemory(User owner) {
        return memoryRepository.save(Memory.builder()
                .user(owner)
                .photoUrl("https://example.com/photo.png")
                .categoryMain("가방")
                .categorySub("백팩")
                .story("테스트 사연")
                .build());
    }

    @Test
    void 이미지가_준비된_잠긴_콘셉트를_열면_크레딧이_차감되고_열람가능해진다() {
        EditionConcept unlocked = editionConceptService.unlock(ownerId, readyConceptId);

        assertThat(unlocked.isUnlocked()).isTrue();
        assertThat(userRepository.findById(ownerId).orElseThrow().getCredit())
                .isEqualTo(100 - creditPolicy.editionUnlockCost());
    }

    @Test
    void 이미_열린_콘셉트를_다시_열면_크레딧_차감_없이_거절된다() {
        editionConceptService.unlock(ownerId, readyConceptId);
        int creditAfterFirstUnlock = userRepository.findById(ownerId).orElseThrow().getCredit();

        assertThatThrownBy(() -> editionConceptService.unlock(ownerId, readyConceptId))
                .isInstanceOf(CustomException.class);
        assertThat(userRepository.findById(ownerId).orElseThrow().getCredit()).isEqualTo(creditAfterFirstUnlock);
    }

    @Test
    void 이미지가_아직_없는_콘셉트를_열려하면_크레딧_차감_없이_거절된다() {
        assertThatThrownBy(() -> editionConceptService.unlock(ownerId, pendingConceptId))
                .isInstanceOf(CustomException.class);
        assertThat(userRepository.findById(ownerId).orElseThrow().getCredit()).isEqualTo(100);
    }

    @Test
    void 크레딧이_부족하면_콘셉트는_잠긴채로_남고_잔액도_그대로다() {
        User poorUser = User.builder()
                .provider(Provider.LOCAL)
                .email("concept-unit-poor@example.com")
                .nickname("콘셉트유닛빈털")
                .password("dummy")
                .build();
        poorUser.grantCredit(creditPolicy.editionUnlockCost() - 1);
        Long poorUserId = userRepository.saveAndFlush(poorUser).getUserId();

        EditionGeneration poorGeneration = generationRepository.save(EditionGeneration.builder()
                .memory(newMemory(poorUser))
                .generationNo(1)
                .storySnapshot("테스트 사연")
                .targetCategoryMain("가방")
                .targetCategorySub("토트백")
                .build());
        EditionConcept poorReadyConcept = conceptRepository.save(EditionConcept.builder()
                .generation(poorGeneration)
                .displayOrder(1)
                .build());
        poorReadyConcept.applyConceptImage(
                "가방", null, "컨셉명", "safe", 1, "베이스", "방식", null, "https://example.com/concept.png", 80);

        assertThatThrownBy(() -> editionConceptService.unlock(poorUserId, poorReadyConcept.getConceptId()))
                .isInstanceOf(CustomException.class);
        assertThat(conceptRepository.findById(poorReadyConcept.getConceptId()).orElseThrow().isUnlocked()).isFalse();
        assertThat(userRepository.findById(poorUserId).orElseThrow().getCredit())
                .isEqualTo(creditPolicy.editionUnlockCost() - 1);
    }

    @Test
    void 다른_사용자의_콘셉트를_열려하면_존재하지_않는_것처럼_거절된다() {
        assertThatThrownBy(() -> editionConceptService.unlock(otherUserId, readyConceptId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 열려있고_준비된_콘셉트를_확정하면_최종_확정된다() {
        EditionConcept concept = conceptRepository.findById(readyConceptId).orElseThrow();
        concept.unlock();
        concept.getGeneration().applyNarrative(List.of("Heritage Edition"), "보증서 문구");

        EditionConcept finalized = editionConceptService.markFinal(ownerId, readyConceptId);

        assertThat(finalized.isFinal()).isTrue();
    }

    @Test
    void 잠긴_콘셉트는_확정할_수_없다() {
        assertThatThrownBy(() -> editionConceptService.markFinal(ownerId, readyConceptId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 이미지가_없는_콘셉트는_열려있어도_확정할_수_없다() {
        EditionConcept concept = conceptRepository.findById(pendingConceptId).orElseThrow();
        concept.unlock();

        assertThatThrownBy(() -> editionConceptService.markFinal(ownerId, pendingConceptId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 에디션명이_아직_없으면_확정할_수_없다() {
        EditionConcept concept = conceptRepository.findById(readyConceptId).orElseThrow();
        concept.unlock();

        assertThatThrownBy(() -> editionConceptService.markFinal(ownerId, readyConceptId))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 같은_배치에서_새로_확정하면_이전_확정은_해제된다() {
        EditionGeneration generation = generationRepository.findById(
                conceptRepository.findById(readyConceptId).orElseThrow().getGeneration().getGenerationId())
                .orElseThrow();
        generation.applyNarrative(List.of("Heritage Edition"), "보증서 문구");

        EditionConcept first = conceptRepository.findById(readyConceptId).orElseThrow();
        first.unlock();
        EditionConcept second = conceptRepository.save(EditionConcept.builder()
                .generation(generation)
                .displayOrder(3)
                .build());
        second.applyConceptImage(
                "가방", null, "컨셉명2", "bold", 3, "베이스2", "방식2", null, "https://example.com/concept2.png", 90);
        second.unlock();

        editionConceptService.markFinal(ownerId, first.getConceptId());
        editionConceptService.markFinal(ownerId, second.getConceptId());

        assertThat(conceptRepository.findById(first.getConceptId()).orElseThrow().isFinal()).isFalse();
        assertThat(conceptRepository.findById(second.getConceptId()).orElseThrow().isFinal()).isTrue();
    }

    @Test
    void 다른_사용자의_콘셉트를_확정하려하면_존재하지_않는_것처럼_거절된다() {
        assertThatThrownBy(() -> editionConceptService.markFinal(otherUserId, readyConceptId))
                .isInstanceOf(CustomException.class);
    }
}
