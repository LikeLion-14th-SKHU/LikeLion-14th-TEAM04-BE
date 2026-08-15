package com.memory_atelier.memory.application;

import com.memory_atelier.ai.AiImageFetcher;
import com.memory_atelier.ai.AiNarrativeClient;
import com.memory_atelier.ai.dto.request.UserInputDto;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.image.S3Uploader;
import com.memory_atelier.memory.api.dto.request.MemorySaveRequestDto;
import com.memory_atelier.memory.domain.ItemOptions;
import com.memory_atelier.memory.domain.Memory;
import com.memory_atelier.memory.domain.repository.MemoryRepository;
import com.memory_atelier.user.domain.User;
import com.memory_atelier.user.domain.repository.UserRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final AiNarrativeClient aiNarrativeClient;
    private final AiImageFetcher aiImageFetcher;

    public MemoryService(
            MemoryRepository memoryRepository,
            UserRepository userRepository,
            S3Uploader s3Uploader,
            AiNarrativeClient aiNarrativeClient,
            AiImageFetcher aiImageFetcher) {
        this.memoryRepository = memoryRepository;
        this.userRepository = userRepository;
        this.s3Uploader = s3Uploader;
        this.aiNarrativeClient = aiNarrativeClient;
        this.aiImageFetcher = aiImageFetcher;
    }

    @Transactional
    public Memory create(Long userId, MemorySaveRequestDto request) {
        if (request.photo() == null || request.photo().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "사진은 필수입니다.");
        }
        ItemOptions.validate(request.categoryMain(), request.categorySub(), request.materialUser(), request.condition());

        User user = userRepository.getReferenceById(userId);
        String photoUrl = upload(request.photo());

        return memoryRepository.save(
                Memory.builder()
                        .user(user)
                        .photoUrl(photoUrl)
                        .categoryMain(request.categoryMain())
                        .categorySub(request.categorySub())
                        .materialUser(request.materialUser())
                        .conditionTags(request.condition())
                        .story(request.story())
                        .editionCategory(request.editionCategory())
                        .build());
    }

    public Page<Memory> getMyMemories(Long userId, Pageable pageable) {
        return memoryRepository.findAllByUserUserIdOrderByMemoryIdDesc(userId, pageable);
    }

    public Memory getMemory(Long userId, Long memoryId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMORY_NOT_FOUND));
        // 존재하지만 남의 소유면, 존재 여부 자체를 노출하지 않기 위해 동일하게 404로 처리한다
        if (!memory.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.MEMORY_NOT_FOUND);
        }
        return memory;
    }

    @Transactional
    public Memory update(Long userId, Long memoryId, MemorySaveRequestDto request) {
        Memory memory = getMemory(userId, memoryId);
        ItemOptions.validate(request.categoryMain(), request.categorySub(), request.materialUser(), request.condition());

        String newPhotoUrl = null;
        if (request.photo() != null && !request.photo().isEmpty()) {
            String oldPhotoUrl = memory.getPhotoUrl();
            newPhotoUrl = upload(request.photo());
            s3Uploader.delete(oldPhotoUrl);
        }

        memory.update(
                newPhotoUrl,
                request.categoryMain(),
                request.categorySub(),
                request.materialUser(),
                request.condition(),
                request.story(),
                request.editionCategory());
        return memory;
    }

    // Stage 1(복합 이해 + 서사 생성) 실행
    // 재질 병합·상태 단서 합집합 규칙은 Memory#applyAnalysis 안에 있다
    @Transactional
    public Memory analyze(Long userId, Long memoryId) {
        Memory memory = getMemory(userId, memoryId);
        ItemOptions.requireCategory(memory.getCategoryMain(), memory.getCategorySub());

        String imageBase64 = aiImageFetcher.fetchAsBase64(memory.getPhotoUrl());
        UserInputDto userInput = toUserInput(memory);
        memory.applyAnalysis(aiNarrativeClient.analyze(imageBase64, userInput));
        return memory;
    }

    // 에디션 생성 시 원문과 다듬은 사연 중 무엇을 쓸지 선택한다
    @Transactional
    public Memory selectStorySource(Long userId, Long memoryId, boolean useRefinedStory) {
        Memory memory = getMemory(userId, memoryId);
        if (useRefinedStory && memory.getStoryRefined() == null) {
            throw new CustomException(ErrorCode.REFINED_STORY_NOT_READY);
        }
        memory.selectStorySource(useRefinedStory);
        return memory;
    }

    @Transactional
    public void delete(Long userId, Long memoryId) {
        Memory memory = getMemory(userId, memoryId);
        s3Uploader.delete(memory.getPhotoUrl());
        memoryRepository.delete(memory);
    }

    private String upload(MultipartFile photo) {
        try {
            return s3Uploader.upload(photo);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private UserInputDto toUserInput(Memory memory) {
        String material = memory.isMaterialUnknown() ? ItemOptions.MATERIAL_UNSELECTED : memory.getMaterialUser();
        List<String> condition = memory.getConditionTags();
        return new UserInputDto(
                new UserInputDto.ClothingCategoryDto(memory.getCategoryMain(), memory.getCategorySub()),
                material,
                condition,
                memory.getStory());
    }
}
