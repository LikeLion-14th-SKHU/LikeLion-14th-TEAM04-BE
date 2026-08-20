package com.memory_atelier.image;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.memory_atelier.global.exception.CustomException;
import com.memory_atelier.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Uploader {

    // JPEG(FF D8 FF)·PNG(89 50 4E 47 0D 0A 1A 0A) 매직바이트. 확장자·Content-Type 헤더는
    // 업로더가 임의로 조작 가능해서 못 믿는다 — 실제 파일 내용을 직접 확인한다
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC =
            {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'};

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 업로드 자체는 항상 S3Config가 설정한 엔드포인트(내부 주소)로 나간다 — 여기 설정된 값은
    // 응답에 실어 보내는 URL을 만들 때만 쓴다. 홈서버 구성에서 앱 컨테이너가 자기 자신의
    // 공개 도메인으로 나갔다가 다시 들어오는 방식(NAT 헤어핀)은 가정용 공유기에서 안 되는
    // 경우가 흔해서, 업로드 경로와 "사용자에게 보여주는 URL"을 분리해뒀다. 비어 있으면(기본값)
    // 기존처럼 amazonS3.getUrl()이 만들어주는 주소를 그대로 쓴다
    @Value("${cloud.aws.s3.public-url:}")
    private String publicUrl;

    public String upload(MultipartFile file) throws IOException {

        byte[] content = file.getBytes();
        // Content-Type은 클라이언트가 보낸 헤더가 아니라 여기서 실제 바이트를 보고 판정한 값만
        // 쓴다 — 그렇지 않으면 Content-Type: text/html로 위장해 올린 파일이 그대로 공개
        // 버킷에서 HTML로 서빙되는 저장형 XSS 경로가 열린다
        String contentType = detectImageContentType(content);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.setContentType(contentType);

        amazonS3.putObject(bucket, fileName, new ByteArrayInputStream(content), metadata);

        return buildUrl(fileName);
    }

    private String detectImageContentType(byte[] content) {
        if (startsWith(content, PNG_MAGIC)) {
            return "image/png";
        }
        if (startsWith(content, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        throw new CustomException(ErrorCode.INVALID_IMAGE_FORMAT);
    }

    private boolean startsWith(byte[] content, byte[] magic) {
        if (content.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private String buildUrl(String fileName) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return amazonS3.getUrl(bucket, fileName).toString();
        }
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + "/" + bucket + "/" + encodedFileName;
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !isOwnedFile(imageUrl)) {
            return; // 우리 S3 소유가 아니면 삭제 시도 자체를 안 함
        }

        try {
            amazonS3.deleteObject(bucket, extractKey(imageUrl));
        } catch (Exception e) {
            System.err.println("S3 파일 삭제 중 오류 발생:" + e.getMessage());
        }
    }

    // photoUrl에는 사용자에게 보여줄 공개 도메인 주소가 저장돼 있는데, 앱 컨테이너 자신이
    // 그 공개 도메인으로 나갔다가 다시 들어오는 건(NAT 헤어핀) 홈서버 구성에서 흔히 막혀 있다.
    // 그래서 그 URL을 HTTP로 재요청하지 않고, S3(MinIO) 내부 엔드포인트로 직접 내려받는다.
    // {@link AiImageFetcher}가 AI 서버에 보낼 이미지를 가져올 때 쓴다
    public byte[] downloadAsBytes(String imageUrl) {
        String key = extractKey(imageUrl);
        try (S3Object s3Object = amazonS3.getObject(bucket, key)) {
            return s3Object.getObjectContent().readAllBytes();
        } catch (AmazonClientException | IOException e) {
            throw new CustomException(ErrorCode.FILE_DOWNLOAD_FAILED, "파일을 내려받는 데 실패했습니다: " + imageUrl);
        }
    }

    // 이 앱의 키는 항상 "UUID_원본파일명" 한 세그먼트뿐이라(하위 디렉터리 없음),
    // URL 마지막 "/" 뒤를 그대로 키로 쓰면 AWS 가상호스팅 스타일(bucket.s3...amazonaws.com/key)과
    // MinIO 등의 path-style(endpoint/bucket/key) 양쪽 다 스타일 무관하게 동작한다
    private String extractKey(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
    }

    private boolean isOwnedFile(String imageUrl) {
        return imageUrl.contains(bucket);
    }
}
