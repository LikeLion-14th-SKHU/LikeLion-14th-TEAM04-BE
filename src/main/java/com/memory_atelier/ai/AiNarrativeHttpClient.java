package com.memory_atelier.ai;

import com.memory_atelier.ai.dto.request.NarrativeRequestDto;
import com.memory_atelier.ai.dto.request.UserInputDto;
import com.memory_atelier.ai.dto.response.AnalysisResultDto;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
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
        } catch (RestClientResponseException e) {
            log.warn("AI 분석 요청 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "AI 분석 요청에 실패했습니다.");
        } catch (RestClientException e) {
            log.warn("AI 분석 요청 실패(연결 오류): {}", e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "AI 분석 요청에 실패했습니다.");
        }
    }
}
