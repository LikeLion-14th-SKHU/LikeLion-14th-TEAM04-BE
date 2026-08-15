package com.memory_atelier.ai;

import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * S3에 올라간 사진 URL을 내려받아 AI 서버가 받는 base64 문자열로 바꾼다.
 * AI 서버는 파일 업로드가 아니라 base64로만 이미지를 받는다({@code /ai/v1/narrative} 요청 스키마 참고).
 *
 * <p>FastAPI 전용 {@code aiRestClient}(baseUrl이 AI 서버로 고정)와는 별개로, S3는 매번 다른 절대
 * URL이라 baseUrl이 없는 일반 RestClient를 쓴다.
 */
@Component
public class AiImageFetcher {

    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10MB

    private final RestClient restClient = RestClient.create();

    public String fetchAsBase64(String photoUrl) {
        byte[] data = restClient.get()
                .uri(photoUrl)
                .retrieve()
                .body(byte[].class);
        if (data == null || data.length == 0) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "사진을 내려받을 수 없습니다: " + photoUrl);
        }
        if (data.length > MAX_BYTES) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "사진 용량이 너무 큽니다: " + photoUrl);
        }
        return Base64.getEncoder().encodeToString(data);
    }
}
