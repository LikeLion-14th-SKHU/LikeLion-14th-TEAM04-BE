package com.memory_atelier.edition.domain;

/**
 * design_spec의 {@code applied_elements} 한 항목 — 옷의 무엇을 제품의 어디에, 왜 적용했는지.
 * 사용자에게는 재창조 근거 설명으로 쓰인다.
 *
 * @param fromElement 원본 옷에서 가져온 요소 (예: "소매의 해진 부분")
 * @param toElement   적용된 MCM 제품의 위치 (예: "스트랩의 스티치 디테일")
 * @param reason      사연과 연결한 근거
 */
public record AppliedElement(String fromElement, String toElement, String reason) {
}
