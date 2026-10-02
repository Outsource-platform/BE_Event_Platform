package org.example.eventplatform.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.dto.BannerRequest;
import org.example.eventplatform.catalog.dto.BannerResponse;
import org.example.eventplatform.catalog.service.BannerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản lý banner trang chủ — chỉ SUPER_ADMIN của sàn. App của khách không gọi
 * đường này mà nhận banner qua {@code /api/public/home_app}.
 */
@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BannerController {

    private final BannerService bannerService;

    /** Gồm cả banner đang tắt, để màn quản trị bật/tắt lại được. */
    @GetMapping
    public ResponseEntity<List<BannerResponse>> list() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping
    public ResponseEntity<BannerResponse> create(@Valid @RequestBody BannerRequest request) {
        return new ResponseEntity<>(bannerService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BannerResponse> update(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
