package com.memory_atelier.recommend.application;

import com.memory_atelier.edition.application.PipelineEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// 3D 변환과 같은 job에서 큐레이션 결과가 도착하면 받아서 저장한다
// EditionPipelineRunner는 비동기 스레드에서 트랜잭션 밖에 있으므로 AFTER_COMMIT을 걸 트랜잭션 자체가 없다
// 그냥 이벤트 발행 직후(같은 스레드)에 처리한다
@Component
@RequiredArgsConstructor
public class RecommendationListener {

    private final RecommendationService recommendationService;

    @EventListener
    public void onCurationReceived(PipelineEvents.CurationReceived event) {
        recommendationService.saveFromCuration(event.conceptId(), event.recommendations());
    }
}
