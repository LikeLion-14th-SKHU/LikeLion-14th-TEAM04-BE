package com.memory_atelier.memory.domain;

import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 아이템 토글 선택지
// AI 서버(FastAPI) {@code user_input} 스키마(ai_pipeline/schemas/user_input.py)와1:1로 맞춘 값이다
// UI 토글이 바뀌면 AI 서버 스키마도 같이 바뀌어야 하는 값이라 한 곳에만 둔다
public final class ItemOptions {

    // 재질 토글의 '선택안함'. 이 값이면 재질 판정을 Stage 1 Vision 추정에 위임한다
    public static final String MATERIAL_UNSELECTED = "선택안함";

    private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>(Map.of(
            "의류", List.of("니트", "가디건", "셔츠", "자켓", "원피스", "후드티", "블라우스", "팬츠"),
            "가방", List.of("핸드백", "토트백", "백팩", "클러치", "트래블"),
            "악세사리", List.of("벨트", "스카프", "지갑", "키링", "헤어밴드")));

    private static final Set<String> MATERIALS = new LinkedHashSet<>(List.of(
            "데님", "가죽", "니트", "울", "면", "린넨", "벨벳", "레이스", MATERIAL_UNSELECTED));

    private static final Set<String> CONDITIONS = new LinkedHashSet<>(List.of(
            "해짐", "색 바램", "얼룩", "늘어남", "새것 같음"));

    private ItemOptions() {
    }

    // 사용자 토글 입력이 AI 서버가 받는 선택지 안에 있는지 확인한다
    // 카테고리는 등록 시점엔 필수가 아니라 둘 다 비어 있으면 통과시키되, 한쪽만 있으면 거절한다
    public static void validate(String categoryMain, String categorySub, String material, List<String> conditions) {
        validateCategory(categoryMain, categorySub);
        validateMaterial(material);
        validateConditions(conditions);
    }

    // AI 분석 호출 직전에는 카테고리가 반드시 있어야 한다 (AI 서버 스키마가 필수 필드)
    public static void requireCategory(String categoryMain, String categorySub) {
        if (!isPresent(categoryMain) || !isPresent(categorySub)) {
            throw invalid("AI 분석을 실행하려면 카테고리(대분류·중분류)를 먼저 선택해야 합니다.");
        }
    }

    private static void validateCategory(String categoryMain, String categorySub) {
        boolean hasMain = isPresent(categoryMain);
        boolean hasSub = isPresent(categorySub);
        if (!hasMain && !hasSub) {
            return;
        }
        if (!hasMain || !hasSub) {
            throw invalid("카테고리는 대분류와 중분류를 함께 선택해야 합니다.");
        }
        List<String> subs = CATEGORIES.get(categoryMain);
        if (subs == null) {
            throw invalid("카테고리 대분류는 %s 중 하나여야 합니다.".formatted(String.join(" / ", CATEGORIES.keySet())));
        }
        if (!subs.contains(categorySub)) {
            throw invalid("'%s'의 중분류는 %s 중 하나여야 합니다.".formatted(categoryMain, String.join(" / ", subs)));
        }
    }

    private static void validateMaterial(String material) {
        if (isPresent(material) && !MATERIALS.contains(material)) {
            throw invalid("재질은 %s 중 하나여야 합니다.".formatted(String.join(" / ", MATERIALS)));
        }
    }

    private static void validateConditions(List<String> conditions) {
        if (conditions == null) {
            return;
        }
        for (String condition : conditions) {
            if (!CONDITIONS.contains(condition)) {
                throw invalid("상태는 %s 중에서만 고를 수 있습니다.".formatted(String.join(" / ", CONDITIONS)));
            }
        }
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static CustomException invalid(String message) {
        return new CustomException(ErrorCode.INVALID_ITEM_OPTION, message);
    }
}
