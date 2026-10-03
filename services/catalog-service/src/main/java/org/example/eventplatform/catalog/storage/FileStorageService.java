package org.example.eventplatform.catalog.storage;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Lưu ảnh người dùng tải lên. Cách làm theo rencity: backend nhận multipart rồi putObject lên FPT S3
 * (path-style), trả URL công khai. Chưa cấu hình S3 thì ghi vào thư mục local và phục vụ qua
 * {@code GET /api/files/local/{name}} — chỉ dùng khi dev vì container build lại sẽ mất file.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final StorageProperties props;
    private S3Client s3;

    /** Trả URL công khai của ảnh vừa lưu. */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn ảnh để tải lên");
        }
        if (file.getSize() > (long) props.getLimitSizeMb() * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh quá lớn, tối đa " + props.getLimitSizeMb() + "MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!IMAGE_EXTS.contains(ext) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ nhận ảnh jpg, png, webp, gif");
        }
        String name = UUID.randomUUID() + "." + ext;
        try {
            if (props.isS3Configured()) {
                String key = "images/" + name;
                s3Client().putObject(
                        PutObjectRequest.builder().bucket(props.getBucketName()).key(key).contentType(contentType).build(),
                        RequestBody.fromBytes(file.getBytes()));
                return publicUrl(key);
            }
            log.warn("S3 chưa cấu hình, lưu ảnh vào thư mục local (chỉ dùng khi dev)");
            Path dir = Paths.get(props.getLocalDir(), "images");
            Files.createDirectories(dir);
            Files.write(dir.resolve(name), file.getBytes());
            return props.getPublicBaseUrl().replaceAll("/$", "") + "/api/files/local/" + name;
        } catch (IOException e) {
            log.error("Không lưu được ảnh", e);
            throw new IllegalStateException("Không lưu được ảnh, thử lại sau");
        }
    }

    /** Đọc ảnh đã lưu local; null nếu không có hoặc tên không hợp lệ (chặn đường dẫn ../). */
    public byte[] readLocal(String name) {
        if (!name.matches("[A-Za-z0-9._-]+")) {
            return null;
        }
        Path file = Paths.get(props.getLocalDir(), "images", name);
        try {
            return Files.exists(file) ? Files.readAllBytes(file) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private synchronized S3Client s3Client() {
        if (s3 == null) {
            s3 = S3Client.builder()
                    .endpointOverride(URI.create(props.getEndpoint()))
                    .region(Region.of(props.getRegion().isBlank() ? "han02" : props.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKey())))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        return s3;
    }

    private String publicUrl(String key) {
        String base = props.getPublicEndpoint().isBlank() ? props.getEndpoint() : props.getPublicEndpoint();
        return base.replaceAll("/$", "") + "/" + key;
    }

    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    @PreDestroy
    void close() {
        if (s3 != null) {
            s3.close();
        }
    }
}
