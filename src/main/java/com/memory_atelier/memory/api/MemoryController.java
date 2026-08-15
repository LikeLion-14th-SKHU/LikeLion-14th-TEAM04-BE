package com.memory_atelier.memory.api;

import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.common.PageResponse;
import com.memory_atelier.global.common.SuccessCode;
import com.memory_atelier.memory.api.dto.request.MemorySaveRequestDto;
import com.memory_atelier.memory.api.dto.request.StorySourceRequestDto;
import com.memory_atelier.memory.api.dto.response.MemoryResponseDto;
import com.memory_atelier.memory.application.MemoryService;
import com.memory_atelier.memory.domain.Memory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/memories")
@RequiredArgsConstructor
@Tag(name = "추억(Memory) API", description = "추억(옷 사진 + 사연) 등록·조회·수정·삭제 및 AI 분석 API")
public class MemoryController {

    private final MemoryService memoryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "추억 등록", description = "사진 파일과 카테고리·재질·상태·사연을 받아 추억을 등록합니다.")
    public ResponseEntity<ApiResponse<MemoryResponseDto>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute MemorySaveRequestDto request) {
        Memory memory = memoryService.create(userId, request);
        return ApiResponse.success(SuccessCode.CREATED, MemoryResponseDto.from(memory));
    }

    @GetMapping
    @Operation(summary = "내 추억 목록 조회")
    public ResponseEntity<ApiResponse<PageResponse<MemoryResponseDto>>> getMyMemories(
            @AuthenticationPrincipal Long userId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(
                SuccessCode.GET_SUCCESS, PageResponse.of(memoryService.getMyMemories(userId, pageable), MemoryResponseDto::from));
    }

    @GetMapping("/{memoryId}")
    @Operation(summary = "추억 상세 조회", description = "본인 소유의 추억만 조회할 수 있습니다.")
    public ResponseEntity<ApiResponse<MemoryResponseDto>> getMemory(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId) {
        return ApiResponse.success(SuccessCode.GET_SUCCESS, MemoryResponseDto.from(memoryService.getMemory(userId, memoryId)));
    }

    @PatchMapping(value = "/{memoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "추억 수정",
            description = "사진은 생략하면 기존 사진을 유지합니다. 사진이나 사연 원문이 바뀌면 이전 AI 분석 결과는 모두 초기화됩니다."
    )
    public ResponseEntity<ApiResponse<MemoryResponseDto>> update(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId,
            @Valid @ModelAttribute MemorySaveRequestDto request) {
        Memory memory = memoryService.update(userId, memoryId, request);
        return ApiResponse.success(SuccessCode.OK, MemoryResponseDto.from(memory));
    }

    @PostMapping("/{memoryId}/analyze")
    @Operation(
            summary = "AI 분석(Stage 1) 실행",
            description = "사진·사연·토글 입력으로 색상·패턴·재질·상태 단서·무드를 분석하고 사연을 다듬습니다. "
                    + "카테고리(대분류·중분류)가 선택돼 있어야 호출할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<MemoryResponseDto>> analyze(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId) {
        Memory memory = memoryService.analyze(userId, memoryId);
        return ApiResponse.success(SuccessCode.OK, MemoryResponseDto.from(memory));
    }

    @PatchMapping("/{memoryId}/story-source")
    @Operation(summary = "생성에 사용할 사연 선택", description = "에디션 생성 시 원문과 AI로 다듬은 사연 중 무엇을 스냅샷으로 쓸지 정합니다.")
    public ResponseEntity<ApiResponse<MemoryResponseDto>> selectStorySource(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId,
            @Valid @RequestBody StorySourceRequestDto request) {
        Memory memory = memoryService.selectStorySource(userId, memoryId, request.useRefinedStory());
        return ApiResponse.success(SuccessCode.OK, MemoryResponseDto.from(memory));
    }

    @DeleteMapping("/{memoryId}")
    @Operation(summary = "추억 삭제")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "추억 id", example = "1") @PathVariable Long memoryId) {
        memoryService.delete(userId, memoryId);
        return ResponseEntity.noContent().build();
    }
}
