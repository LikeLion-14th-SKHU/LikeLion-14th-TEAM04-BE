package com.memory_atelier.global.config;

import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {

    @Value("${ai.base-url}")
    private String baseUrl;

    @Bean
    public RestClient aiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        ClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // /ai/v1/narrative 등 동기 엔드포인트는 내부에서 LLM(Claude/Gemini) 호출을 기다리므로
        // 실측 18초+ 걸리는 경우가 있었다(테스트용 소형 이미지 기준) — 10초는 너무 짧아 AI 서버가
        // 200을 반환하기 전에 매번 타임아웃됐다.
        ((JdkClientHttpRequestFactory) requestFactory).setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new CustomException(
                                    ErrorCode.EXTERNAL_API_ERROR,
                                    "AI 서버 응답 실패: " + response.getStatusCode());
                        })
                .build();
    }
}
