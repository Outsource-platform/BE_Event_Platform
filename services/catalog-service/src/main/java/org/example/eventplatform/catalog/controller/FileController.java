package org.example.eventplatform.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.storage.FileStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /** Phục vụ ảnh lưu local khi chưa cấu hình S3 (dev). */
    @GetMapping("/local/{name}")
    public ResponseEntity<byte[]> local(@PathVariable String name) {
        byte[] data = storage.readLocal(name);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        String lower = name.toLowerCase();
        MediaType type = lower.endsWith(".png") ? MediaType.IMAGE_PNG
                : lower.endsWith(".gif") ? MediaType.IMAGE_GIF
                : lower.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(Duration.ofDays(30))).body(data);
    }
}
