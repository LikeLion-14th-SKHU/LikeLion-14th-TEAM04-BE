package com.memory_atelier.ai;

import com.memory_atelier.ai.dto.request.UserInputDto;
import com.memory_atelier.ai.dto.response.AnalysisResultDto;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** AI 서버 없이도 Memory 도메인을 개발·테스트할 수 있게 하는 가짜 구현체. */
@Component
@ConditionalOnProperty(name = "ai.pipeline.client", havingValue = "mock")
public class AiNarrativeMockClient implements AiNarrativeClient {

    @Override
    public AnalysisResultDto analyze(String imageBase64, UserInputDto userInput) {
        AnalysisResultDto.Visual visual = new AnalysisResultDto.Visual(
                List.of("#4A6B8A", "#D9CBB3"),
                "무지",
                userInput.material(),
                "가죽",
                userInput.material(),
                userInput.condition(),
                List.of("빈티지", "차분함"));
        AnalysisResultDto.Story story = new AnalysisResultDto.Story(
                userInput.story() + " (mock으로 다듬어진 버전)",
                "mock 내부 해석",
                List.of("설렘", "그리움"));
        return new AnalysisResultDto(
                visual, story, List.of("Mock Heritage Edition", "Mock Memory Edition"), "mock 보증서 문구");
    }
}
