package org.example.eventplatform.catalog.storage;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cấu hình lưu file. Thiếu thông tin S3 thì {@link #isS3Configured()} false và dùng thư mục local. */
@Component
@Getter
public class StorageProperties {

    @Value("${fpt-s3.access-key-id:}")
    private String accessKeyId;
    @Value("${fpt-s3.secret-access-key:}")
    private String secretAccessKey;
    @Value("${fpt-s3.endpoint:}")
    private String endpoint;
    @Value("${fpt-s3.region:han02}")
    private String region;
    @Value("${fpt-s3.bucket-name:}")
    private String bucketName;
    @Value("${fpt-s3.public-endpoint:}")
    private String publicEndpoint;
    @Value("${fpt-s3.limit-size-mb:10}")
    private int limitSizeMb;
    @Value("${upload.local-dir:uploads}")
    private String localDir;
    @Value("${upload.public-base-url:}")
    private String publicBaseUrl;

    public boolean isS3Configured() {
        return !accessKeyId.isBlank() && !secretAccessKey.isBlank() && !endpoint.isBlank() && !bucketName.isBlank();
    }
}
