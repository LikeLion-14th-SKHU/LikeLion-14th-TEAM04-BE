package com.memory_atelier.image;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Uploader {

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

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);

        return buildUrl(fileName);
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
            // 이 앱의 키는 항상 "UUID_원본파일명" 한 세그먼트뿐이라(하위 디렉터리 없음),
            // URL 마지막 "/" 뒤를 그대로 키로 쓰면 AWS 가상호스팅 스타일(bucket.s3...amazonaws.com/key)과
            // MinIO 등의 path-style(endpoint/bucket/key) 양쪽 다 스타일 무관하게 동작한다
            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            amazonS3.deleteObject(bucket, decodedFileName);
        } catch (Exception e) {
            System.err.println("S3 파일 삭제 중 오류 발생:" + e.getMessage());
        }
    }

    private boolean isOwnedFile(String imageUrl) {
        return imageUrl.contains(bucket);
    }
}
