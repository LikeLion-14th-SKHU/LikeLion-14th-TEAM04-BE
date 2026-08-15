package com.memory_atelier.ai;

import com.memory_atelier.ai.dto.request.UserInputDto;
import com.memory_atelier.ai.dto.response.AnalysisResultDto;

/**
 * FastAPI Stage 1({@code POST /ai/v1/narrative}, 동기) 호출 창구.
 * {@link AiPipelineClient}(비동기 job 기반 pipeline-run)와는 별개 지점이다 — Memory가 사진·사연을
 * 최초 분석할 때만 쓰고, Edition Generation은 이 결과를 재사용하지 않고 자체적으로 pipeline-run을 돈다.
 */
public interface AiNarrativeClient {

    /** 사진+사연 전체 분석. */
    AnalysisResultDto analyze(String imageBase64, UserInputDto userInput);
}
