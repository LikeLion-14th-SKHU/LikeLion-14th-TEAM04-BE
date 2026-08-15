package com.memory_atelier.edition.application;

public final class PipelineEvents {

    private PipelineEvents() {
    }

    /** 생성 배치가 커밋된 뒤에 발행된다 — 커밋 전에 비동기 스레드가 아직 없는 행을 조회하는 걸 막기 위함. */
    public record GenerationCreated(Long generationId) {
    }
}
