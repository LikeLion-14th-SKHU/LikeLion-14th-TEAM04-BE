package com.memory_atelier.edition.application;

import java.util.List;

public final class PipelineEvents {

    private PipelineEvents() {
    }

    // 생성 배치가 커밋된 뒤에 발행된다 — 커밋 전에 비동기 스레드가 아직 없는 행을 조회하는 걸 막기 위함
    public record GenerationCreated(Long generationId) {
    }

    // 콘셉트가 최종 확정(및 보증서 발급)된 뒤에 발행된다 — 3D 변환을 커밋 후 비동기로 시작하기 위함
    public record ConceptFinalized(Long conceptId) {
    }

    // 3D 변환과 같은 job에서 큐레이션(Stage 5) 결과가 함께 도착했을 때 발행된다
    // payload를 recommend 도메인의 타입이 아니라 여기(edition 패키지)에 정의하는 이유는 의존방향 때문이다
    // recommend가 edition을 참조하는 건({@code Recommendation}이 콘셉트를 가리켜야 하니까) 불가피하지만,
    // 이 이벤트 타입을 recommend 패키지에 두면 edition이 그걸 import하기 위해 반대 방향(edition → recommend)까지 생긴다
    // 이벤트 타입을 edition 쪽에 두면 recommend만 이걸 import하면 되므로 한쪽 방향만 남는다.
    public record CurationReceived(Long conceptId, List<RecommendationPayload> recommendations) {

        public record RecommendationPayload(
                String productId, String nameKr, String reason, String tagline, String imageUrl) {
        }
    }
}
