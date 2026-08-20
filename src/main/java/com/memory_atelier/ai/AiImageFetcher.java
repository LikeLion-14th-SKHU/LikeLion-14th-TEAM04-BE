package com.memory_atelier.ai;

import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import com.memory_atelier.image.S3Uploader;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * S3(MinIO)에 올라간 사진을 내려받아 AI 서버가 받는 base64 문자열로 바꾼다.
 * AI 서버는 파일 업로드가 아니라 base64로만 이미지를 받는다({@code /ai/v1/narrative} 요청 스키마 참고).
 *
 * <p>{@code Memory.photoUrl}에는 사용자에게 보여줄 공개 도메인 URL이 저장돼 있지만, 앱 컨테이너
 * 자신이 그 공개 도메인으로 나갔다가 다시 들어오는 건(NAT 헤어핀) 홈서버 구성에서 흔히 막혀
 * 있다({@link S3Uploader} 참고) — 그래서 HTTP로 그 URL을 재요청하지 않고 {@link S3Uploader}를
 * 통해 S3(MinIO) 내부 엔드포인트로 직접 내려받는다.
 */
@Component
@RequiredArgsConstructor
public class AiImageFetcher {

    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10MB

    private final S3Uploader s3Uploader;

    public String fetchAsBase64(String photoUrl) {
        byte[] data = s3Uploader.downloadAsBytes(photoUrl);
        if (data.length == 0) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "사진을 내려받을 수 없습니다: " + photoUrl);
        }
        if (data.length > MAX_BYTES) {
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR, "사진 용량이 너무 큽니다: " + photoUrl);
        }
        return Base64.getEncoder().encodeToString(data);
    }
}
