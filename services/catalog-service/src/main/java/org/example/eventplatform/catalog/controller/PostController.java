package org.example.eventplatform.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.dto.PageResult;
import org.example.eventplatform.catalog.dto.PostRequest;
import org.example.eventplatform.catalog.dto.PostResponse;
import org.example.eventplatform.catalog.dto.PostStatusRequest;
import org.example.eventplatform.catalog.dto.PostSummary;
import org.example.eventplatform.catalog.dto.PushQuota;
import org.example.eventplatform.catalog.dto.SitemapItem;
import org.example.eventplatform.catalog.entity.PostStatus;
import org.example.eventplatform.catalog.service.PostService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/**
 * Ba nhóm đường: công khai (trang Tin tức, không cần đăng nhập), của đơn vị ({@code /mine}, chỉ bài của
 * tenant mình) và của Super Admin ({@code /admin}, mọi bài, kể cả sửa SEO hộ đơn vị).
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // Trang Tin tức công khai cũng được web cache 60 giây (ISR); thêm header để CDN/nginx giữ được cùng thời gian.
    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    // ===== Công khai =====

    @GetMapping("/public")
    public ResponseEntity<PageResult<PostSummary>> listPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(postService.listPublished(page, size, tenantId));
    }

    @GetMapping("/public/sitemap")
    public ResponseEntity<List<SitemapItem>> sitemap() {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(postService.sitemap());
    }

    @GetMapping("/public/{slug}")
    public ResponseEntity<PostResponse> getPublic(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(postService.getPublished(slug));
    }

    // ===== Đơn vị =====

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResult<PostSummary>> listMine(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.listForTenant(principal.tenantId(), page, size));
    }

    @GetMapping("/mine/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> getMine(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(postService.get(id, principal.tenantId()));
    }

    @PostMapping("/mine")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> createMine(@AuthenticationPrincipal JwtPrincipal principal,
                                                   @Valid @RequestBody PostRequest request) {
        return new ResponseEntity<>(postService.create(request, principal.tenantId(), principal.userId()), HttpStatus.CREATED);
    }

    @PutMapping("/mine/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> updateMine(@AuthenticationPrincipal JwtPrincipal principal,
                                                   @PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.update(id, request, principal.tenantId()));
    }

    @PatchMapping("/mine/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> setMineStatus(@AuthenticationPrincipal JwtPrincipal principal,
                                                      @PathVariable Long id, @Valid @RequestBody PostStatusRequest request) {
        return ResponseEntity.ok(postService.setStatus(id, request.getStatus(), principal.tenantId()));
    }

    @GetMapping("/mine/push-quota")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PushQuota> pushQuota(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(postService.pushQuota(principal.tenantId()));
    }

    @PostMapping("/mine/{id}/push")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PushQuota> pushMine(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(postService.push(id, principal.tenantId()));
    }

    @DeleteMapping("/mine/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMine(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        postService.delete(id, principal.tenantId());
        return ResponseEntity.noContent().build();
    }

    // ===== Super Admin =====

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PageResult<PostSummary>> search(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.search(status, tenantId, q, page, size));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PostResponse> getAny(@PathVariable Long id) {
        return ResponseEntity.ok(postService.get(id, null));
    }

    /** Bài của sàn (không thuộc đơn vị nào). */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PostResponse> createPlatform(@AuthenticationPrincipal JwtPrincipal principal,
                                                       @Valid @RequestBody PostRequest request) {
        return new ResponseEntity<>(postService.create(request, null, principal.userId()), HttpStatus.CREATED);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PostResponse> updateAny(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.update(id, request, null));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PostResponse> setAnyStatus(@PathVariable Long id, @Valid @RequestBody PostStatusRequest request) {
        return ResponseEntity.ok(postService.setStatus(id, request.getStatus(), null));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAny(@PathVariable Long id) {
        postService.delete(id, null);
        return ResponseEntity.noContent().build();
    }
}
