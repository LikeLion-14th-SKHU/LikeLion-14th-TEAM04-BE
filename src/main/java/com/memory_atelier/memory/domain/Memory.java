package com.memory_atelier.memory.domain;

import com.memory_atelier.ai.dto.response.AnalysisResultDto;
import com.memory_atelier.global.entity.BaseTimeEntity;
import com.memory_atelier.global.entity.StringListConverter;
import com.memory_atelier.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memory extends BaseTimeEntity {

    // 다듬은 사연·내부 해석 컬럼 길이. 원문(500)보다 넉넉한 건 맞춤법 정리로 길이가 늘 수 있어서다
    public static final int REFINED_STORY_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memory_id")
    private Long memoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String photoUrl;

    // ── 사용자 토글 입력 (AI user_input 스키마 대응) ──────────────────

    private String categoryMain;

    private String categorySub;

    // 사용자가 고른 재질. {@link ItemOptions#MATERIAL_UNSELECTED}면 Vision 추정값을 채택한다
    private String materialUser;

    @Convert(converter = StringListConverter.class)
    @Column(name = "condition_tags", length = 500)
    private List<String> conditionTags = List.of();

    @Column(nullable = false, length = 500)
    private String story;

    // 목표 MCM 에디션 카테고리. null이면 AI가 후보마다 카테고리를 자동 제안한다
    private String editionCategory;

    // ── Stage 1 분석 결과 ────────────────────────────────────

    @Convert(converter = StringListConverter.class)
    @Column(length = 500)
    private List<String> colorPalette = List.of();

    private String pattern;

    private String materialEstimate;

    // 병합 규칙(사용자 선택 우선, '선택안함'이면 Vision 추정값) 적용 결과
    private String materialFinal;

    @Convert(converter = StringListConverter.class)
    @Column(length = 500)
    private List<String> conditionCues = List.of();

    @Convert(converter = StringListConverter.class)
    @Column(length = 500)
    private List<String> vibeKeywords = List.of();

    // {@code story.polished} — 공유용으로 다듬은 사연
    @Column(length = REFINED_STORY_MAX_LENGTH)
    private String storyRefined;

    // {@code story.interpretation} — 내부용 해석. 응답 DTO에 실어 노출하지 않는다
    @Column(length = REFINED_STORY_MAX_LENGTH)
    private String storyInterpretation;

    @Convert(converter = StringListConverter.class)
    @Column(length = 500)
    private List<String> emotionKeywords = List.of();

    @Column(nullable = false)
    private boolean useRefinedStory;

    @Builder
    private Memory(
            User user,
            String photoUrl,
            String categoryMain,
            String categorySub,
            String materialUser,
            List<String> conditionTags,
            String story,
            String editionCategory) {
        this.user = user;
        this.photoUrl = photoUrl;
        this.categoryMain = categoryMain;
        this.categorySub = categorySub;
        this.materialUser = materialUser;
        this.conditionTags = (conditionTags != null) ? conditionTags : List.of();
        this.story = story;
        this.editionCategory = editionCategory;
        this.useRefinedStory = false;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getUserId().equals(userId);
    }

    public Long getOwnerId() {
        return this.user.getUserId();
    }

    // 에디션 생성 배치에 스냅샷으로 고정할 사연 텍스트
    public String resolveStoryForGeneration() {
        return (useRefinedStory && storyRefined != null) ? storyRefined : story;
    }

    // AI 분석(Stage 1)이 완료됐는지
    // Edition Generation은 이 상태를 전제로 한다
    public boolean isAnalyzed() {
        return materialFinal != null;
    }

    public void update(
            String photoUrl,
            String categoryMain,
            String categorySub,
            String materialUser,
            List<String> conditionTags,
            String story,
            String editionCategory) {
        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
        this.categoryMain = categoryMain;
        this.categorySub = categorySub;
        this.materialUser = materialUser;
        this.conditionTags = (conditionTags != null) ? conditionTags : List.of();
        this.story = story;
        this.editionCategory = editionCategory;
        // 사진이나 사연 원문이 바뀌면 그걸로 뽑은 분석 결과는 더 이상 유효하지 않다.
        clearAnalysis();
    }

    // Stage 1 결과를 반영한다
    // 재질은 사용자 선택값을 우선 채택하고 '선택안함'일 때만 Vision 추정값을 쓴다
    // 상태는 사용자 토글과 Vision 단서의 합집합이며, 순서를 유지한 채 중복을 없앤다
    public void applyAnalysis(AnalysisResultDto analysis) {
        AnalysisResultDto.Visual visual = analysis.visual();
        AnalysisResultDto.Story story = analysis.story();

        this.colorPalette = orEmpty(visual.colorPalette());
        this.pattern = visual.pattern();
        this.materialEstimate = visual.materialEstimate();
        this.materialFinal = resolveMaterial(visual.materialEstimate());
        this.conditionCues = mergeConditions(visual.conditionCues());
        this.vibeKeywords = orEmpty(visual.vibeKeywords());
        this.storyRefined = truncateRefined(story.polished());
        this.storyInterpretation = truncateRefined(story.interpretation());
        this.emotionKeywords = orEmpty(story.emotionKeywords());
    }

    public void selectStorySource(boolean useRefinedStory) {
        this.useRefinedStory = useRefinedStory;
    }

    // 재질 판정을 Vision에 위임한 경우인지
    public boolean isMaterialUnknown() {
        return materialUser == null || materialUser.isBlank() || ItemOptions.MATERIAL_UNSELECTED.equals(materialUser);
    }

    private String resolveMaterial(String materialEstimate) {
        return isMaterialUnknown() ? materialEstimate : materialUser;
    }

    private List<String> mergeConditions(List<String> visionCues) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(orEmpty(conditionTags));
        merged.addAll(orEmpty(visionCues));
        return new ArrayList<>(merged);
    }

    private static List<String> orEmpty(List<String> values) {
        return (values != null) ? values : List.of();
    }

    private void clearAnalysis() {
        this.colorPalette = List.of();
        this.pattern = null;
        this.materialEstimate = null;
        this.materialFinal = null;
        this.conditionCues = List.of();
        this.vibeKeywords = List.of();
        this.storyRefined = null;
        this.storyInterpretation = null;
        this.emotionKeywords = List.of();
        this.useRefinedStory = false;
    }

    private static String truncateRefined(String text) {
        if (text == null || text.length() <= REFINED_STORY_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, REFINED_STORY_MAX_LENGTH);
    }
}
