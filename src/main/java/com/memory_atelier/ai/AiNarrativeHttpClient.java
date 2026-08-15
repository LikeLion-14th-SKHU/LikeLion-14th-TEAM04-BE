package com.memory_atelier.ai;

import com.memory_atelier.ai.dto.request.NarrativeRequestDto;
import com.memory_atelier.ai.dto.request.UserInputDto;
import com.memory_atelier.ai.dto.response.AnalysisResultDto;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.pipeline.client", havingValue = "http", matchIfMissing = true)
public class AiNarrativeHttpClient implements AiNarrativeClient {

    private final RestClient aiRestClient;

    @Override
    public AnalysisResultDto analyze(String imageBase64, UserInputDto userInput) {
        try {
            return aiRestClient.post()
                    .uri("/ai/v1/narrative")
                    .body(new NarrativeRequestDto(imageBase64, userInput))
                    .retrieve()
                    .body(AnalysisResultDto.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "AI 분석 요청에 실패했습니다.");
        }
    }
}
