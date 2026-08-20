package com.memory_atelier.edition.domain;

import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 에디션 생성 요청의 목표(target) 카테고리 선택지 — MCM이 실제로 만드는 제품군 기준
// AI 서버(FastAPI) target_category 계약과 1:1로 맞춘 값이므로 한 곳에만 둔다
public final class TargetCategoryOptions {

    // ai_pipeline/services/brand_assets.py의 RECREATION_TAXONOMY와 1:1로 맞춘 값이다
    private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>(Map.of(
            "의류", List.of("니트", "가디건", "셔츠", "자켓", "스커트", "후드티", "티셔츠", "팬츠"),
            "가방", List.of("핸드백", "토트백", "백팩", "클러치", "트래블"),
            "악세사리", List.of("벨트", "스카프", "지갑", "키링", "헤어밴드")));

    private TargetCategoryOptions() {
    }

    public static void validate(String categoryMain, String categorySub) {
        List<String> subs = CATEGORIES.get(categoryMain);
        if (subs == null) {
            throw invalid("목표 카테고리 대분류는 %s 중 하나여야 합니다.".formatted(String.join(" / ", CATEGORIES.keySet())));
        }
        if (!subs.contains(categorySub)) {
            throw invalid("'%s'의 목표 카테고리 중분류는 %s 중 하나여야 합니다.".formatted(categoryMain, String.join(" / ", subs)));
        }
    }

    private static CustomException invalid(String message) {
        return new CustomException(ErrorCode.INVALID_TARGET_CATEGORY, message);
    }
}
