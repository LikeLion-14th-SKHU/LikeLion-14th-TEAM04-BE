package com.memory_atelier.edition.application;

public final class PipelineEvents {

    private PipelineEvents() {
    }

    // 생성 배치가 커밋된 뒤에 발행된다 — 커밋 전에 비동기 스레드가 아직 없는 행을 조회하는 걸 막기 위함
    public record GenerationCreated(Long generationId) {
    }

    // 콘셉트가 최종 확정(및 보증서 발급)된 뒤에 발행된다 — 3D 변환을 커밋 후 비동기로 시작하기 위함
    public record ConceptFinalized(Long conceptId) {
    }
}
