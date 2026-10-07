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
import java.nio.file.StandardCopyOption;
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
    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "mov", "m4v");

    private final StorageProperties props;
    private final MediaTranscoder transcoder;
    // Nén video chạy lần lượt từng cái để không ngốn CPU và bộ nhớ của máy chủ dùng chung.
    private final java.util.concurrent.ExecutorService videoQueue = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "video-compress");
        t.setDaemon(true);
        return t;
    });
    private S3Client s3;

    /**
     * Trả URL công khai của ảnh vừa lưu. Ảnh jpg/png được đổi sang WebP (cạnh dài tối đa 1600px) để nhẹ hơn nhiều mà vẫn nét;
     * gif giữ nguyên để không mất hoạt ảnh, và không có ffmpeg thì lưu bản gốc.
     */
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
        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("img-");
            Path in = tmp.resolve("in." + ext);
            file.transferTo(in);
            Path stored = in;
            String storedExt = ext;
            String storedType = contentType;
            if (!"gif".equals(ext) && !"webp".equals(ext)) {
                Path out = tmp.resolve("out.webp");
                if (transcoder.toWebp(in, out) && Files.size(out) > 0) {
                    stored = out;
                    storedExt = "webp";
                    storedType = "image/webp";
                }
            }
            String name = UUID.randomUUID() + "." + storedExt;
            if (props.isS3Configured()) {
                String key = "images/" + name;
                s3Client().putObject(
                        PutObjectRequest.builder().bucket(props.getBucketName()).key(key).contentType(storedType).build(),
                        RequestBody.fromFile(stored));
                return publicUrl(key);
            }
            log.warn("S3 chưa cấu hình, lưu ảnh vào thư mục local (chỉ dùng khi dev)");
            Path dir = Paths.get(props.getLocalDir(), "images");
            Files.createDirectories(dir);
            Files.copy(stored, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return props.getPublicBaseUrl().replaceAll("/$", "") + "/api/files/local/" + name;
        } catch (IOException e) {
            log.error("Không lưu được ảnh", e);
            throw new IllegalStateException("Không lưu được ảnh, thử lại sau");
        } finally {
            deleteQuietly(tmp);
        }
    }

    /**
     * Lưu video giới thiệu show. Bản gốc được lưu ngay để trả URL liền, rồi nén nền (một video một lúc) sang MP4 H.264
     * 720p và ghi đè đúng URL đó; trong lúc chờ, người xem vẫn phát được bản gốc. Chỉ nhận mp4, mov, m4v.
     */
    public String storeVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn video để tải lên");
        }
        if (file.getSize() > (long) props.getVideoLimitMb() * 1024 * 1024) {
            throw new IllegalArgumentException("Video quá lớn, tối đa " + props.getVideoLimitMb() + "MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!VIDEO_EXTS.contains(ext) || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("Chỉ nhận video mp4, mov");
        }
        // URL luôn kết thúc .mp4 vì bản nén cuối cùng là MP4, kể cả khi bản gốc là .mov của iPhone.
        String name = UUID.randomUUID() + ".mp4";
        Path tmp = null;
        boolean handedOver = false;
        try {
            tmp = Files.createTempDirectory("vid-");
            Path in = tmp.resolve("in." + ext);
            file.transferTo(in);
            String url = publishVideo(in, name, contentType);
            if (transcoder.isAvailable()) {
                Path workDir = tmp;
                videoQueue.submit(() -> compressVideo(workDir, in, name));
                handedOver = true;
            }
            return url;
        } catch (IOException e) {
            log.error("Không lưu được video", e);
            throw new IllegalStateException("Không lưu được video, thử lại sau");
        } finally {
            if (!handedOver) {
                deleteQuietly(tmp);
            }
        }
    }

    private String publishVideo(Path source, String name, String contentType) throws IOException {
        if (props.isS3Configured()) {
            String key = "videos/" + name;
            s3Client().putObject(
                    PutObjectRequest.builder().bucket(props.getBucketName()).key(key).contentType(contentType).build(),
                    RequestBody.fromFile(source));
            return publicUrl(key);
        }
        log.warn("S3 chưa cấu hình, lưu video vào thư mục local");
        Path dir = Paths.get(props.getLocalDir(), "videos");
        Files.createDirectories(dir);
        Files.copy(source, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return props.getPublicBaseUrl().replaceAll("/$", "") + "/api/files/local/" + name;
    }

    private void compressVideo(Path workDir, Path in, String name) {
        try {
            Path out = workDir.resolve("out.mp4");
            if (!transcoder.toMp4(in, out) || Files.size(out) == 0) {
                log.warn("Nén video {} không thành công, giữ bản gốc", name);
                return;
            }
            if (Files.size(out) >= Files.size(in) && in.toString().endsWith(".mp4")) {
                log.info("Video {} đã gọn, giữ bản gốc ({} -> {} byte)", name, Files.size(in), Files.size(out));
                return;
            }
            if (props.isS3Configured()) {
                s3Client().putObject(
                        PutObjectRequest.builder().bucket(props.getBucketName()).key("videos/" + name).contentType("video/mp4").build(),
                        RequestBody.fromFile(out));
            } else {
                Path dir = Paths.get(props.getLocalDir(), "videos");
                Path tmpTarget = dir.resolve(name + ".part");
                Files.copy(out, tmpTarget, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tmpTarget, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            log.info("Đã nén video {}: {} -> {} byte", name, Files.size(in), Files.size(out));
        } catch (Exception e) {
            log.error("Lỗi khi nén video {}", name, e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // dọn dẹp tạm, bỏ qua lỗi
        }
    }

    /** Tìm tệp đã lưu local (ảnh hoặc video); null nếu không có hoặc tên không hợp lệ (chặn đường dẫn ../). */
    public Path findLocal(String name) {
        if (!name.matches("[A-Za-z0-9._-]+")) {
            return null;
        }
        for (String folder : new String[]{"images", "videos"}) {
            Path file = Paths.get(props.getLocalDir(), folder, name);
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        return null;
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
        videoQueue.shutdown();
        if (s3 != null) {
            s3.close();
        }
    }
}
