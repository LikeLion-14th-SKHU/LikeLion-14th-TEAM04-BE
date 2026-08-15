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
        ((JdkClientHttpRequestFactory) requestFactory).setReadTimeout(Duration.ofSeconds(10));

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
