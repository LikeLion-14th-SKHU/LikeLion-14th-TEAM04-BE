package com.memory_atelier.image;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file) throws IOException {

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);

        return amazonS3.getUrl(bucket, fileName).toString();
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !isOwnedFile(imageUrl)) {
            return; // 우리 S3 소유가 아니면 삭제 시도 자체를 안 함
        }

        try {
            String fileName = imageUrl.substring(imageUrl.indexOf(".com/") + 5);
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
