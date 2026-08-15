package com.memory_atelier.edition.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 생성 배치 커밋 이후에 비동기 파이프라인을 착수한다. 커밋 전에 띄우면 비동기 스레드가 아직
 * DB에 없는 행을 찾게 되므로 반드시 AFTER_COMMIT이어야 한다.
 */
@Component
@RequiredArgsConstructor
public class EditionPipelineListener {

    private final EditionPipelineRunner runner;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGenerationCreated(PipelineEvents.GenerationCreated event) {
        runner.runGeneration(event.generationId());
    }
}
