package org.example.eventplatform.event.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.HomeAppResponse;
import org.example.eventplatform.event.dto.PublicPackageResponse;
import org.example.eventplatform.event.dto.PublicShowPage;
import org.example.eventplatform.event.dto.PublicTroupeResponse;
import org.example.eventplatform.event.service.PublicCatalogService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** Không yêu cầu đăng nhập — khách vãng lai duyệt sàn trước khi tạo tài khoản. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicCatalogController {

    // Dữ liệu công khai đổi chậm: cho phép trình duyệt/CDN/nginx giữ ngắn hạn để giảm tải về backend.
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic();

    private final PublicCatalogService publicCatalogService;

    /** Gộp dữ liệu trang chủ vào một lần gọi. */
    @GetMapping("/home_app")
    public ResponseEntity<HomeAppResponse> getHomeApp() {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicCatalogService.getHomeApp());
    }

    @GetMapping("/troupes")
    public ResponseEntity<List<PublicTroupeResponse>> listTroupes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String ward) {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicCatalogService.listTroupes(category, province, ward));
    }

    @GetMapping("/troupes/{id}")
    public ResponseEntity<PublicTroupeResponse> getTroupe(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicCatalogService.getTroupe(id));
    }

    @GetMapping("/shows")
    public ResponseEntity<PublicShowPage> listShows(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicCatalogService.listShows(page, size));
    }

    @GetMapping("/packages")
    public ResponseEntity<List<PublicPackageResponse>> listPackages() {
        return ResponseEntity.ok().cacheControl(CACHE).body(publicCatalogService.listPackages());
    }
}
