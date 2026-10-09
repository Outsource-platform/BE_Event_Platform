package org.example.eventplatform.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.storage.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storage;

    /** Tải ảnh lên (ảnh bìa bài viết, banner...). Super Admin và quản trị viên đơn vị đều dùng được. */
    @PostMapping("/images")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(Map.of("url", storage.storeImage(image)));
    }

    /** Ảnh gửi trong tin nhắn: khách và trưởng đoàn đều dùng được (khác /images chỉ cho quản trị). Cũng đổi sang WebP. */
    @PostMapping("/chat-images")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<Map<String, String>> uploadChatImage(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(Map.of("url", storage.storeImage(image)));
    }

    /** Tải video giới thiệu show lên. Chỉ quản trị đơn vị và Super Admin. */
    @PostMapping("/videos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("video") MultipartFile video) {
        return ResponseEntity.ok(Map.of("url", storage.storeVideo(video)));
    }

    /**
     * Phục vụ ảnh và video lưu local khi chưa cấu hình S3. Trả Resource để Spring tự xử lý header Range,
     * bắt buộc với video vì trình phát trên iOS/Android tải từng đoạn.
     */
    @GetMapping("/local/{name}")
    public ResponseEntity<Resource> local(@PathVariable String name) {
        Path file = storage.findLocal(name);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        String lower = name.toLowerCase();
        MediaType type = lower.endsWith(".png") ? MediaType.IMAGE_PNG
                : lower.endsWith(".gif") ? MediaType.IMAGE_GIF
                : lower.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : lower.endsWith(".mp4") || lower.endsWith(".m4v") ? MediaType.parseMediaType("video/mp4")
                : lower.endsWith(".mov") ? MediaType.parseMediaType("video/quicktime")
                : lower.endsWith(".webm") ? MediaType.parseMediaType("video/webm")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                .body(new FileSystemResource(file));
    }
}
