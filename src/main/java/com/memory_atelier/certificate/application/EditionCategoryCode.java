package com.memory_atelier.certificate.application;

import java.util.Map;

// 에디션 번호 맨 앞에 붙는 카테고리 코드 1글자. concept.category는 AI가 후보마다 자유롭게 내는
// 값이 될 수 있어서(자동 제안 모드), 알려진 값만 의미 있는 글자로 매핑하고 매핑에 없는 카테고리는
// 고정 폴백 코드로 떨어뜨린다 — 매핑에 없는 카테고리가 와도 채번 자체는 절대 실패하지 않는다
final class EditionCategoryCode {

    static final String FALLBACK = "X";

    private static final Map<String, String> CODES = Map.of(
            "가방", "B",
            "지갑", "W",
            "키링", "K",
            "벨트", "L",
            "스카프", "S",
            "헤어밴드", "H");

    private EditionCategoryCode() {
    }

    static String resolve(String category) {
        return CODES.getOrDefault(category, FALLBACK);
    }
}
