package org.example.eventplatform.catalog.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.dto.internal.VendorProfileSummaryResponse;
import org.example.eventplatform.catalog.entity.ServiceCategory;
import org.example.eventplatform.catalog.repository.BannerRepository;
import org.example.eventplatform.catalog.entity.VendorProfile;
import org.example.eventplatform.catalog.repository.ServiceCategoryRepository;
import org.example.eventplatform.catalog.repository.VendorProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service only — guarded by {@code X-Internal-Token}.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final VendorProfileRepository vendorProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final BannerRepository bannerRepository;

    /** Danh mục dịch vụ đang mở — event-service dùng để dựng trang chủ sàn khách. */
    @GetMapping("/service-categories")
    public ResponseEntity<List<ServiceCategorySummary>> getActiveCategories() {
        List<ServiceCategorySummary> categories = serviceCategoryRepository.findByActiveTrue().stream()
                .map(c -> new ServiceCategorySummary(c.getId(), c.getCode(), c.getName(), c.getDescription()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    /** Banner đang bật, đã sắp thứ tự — event-service gộp vào home_app. */
    @GetMapping("/banners")
    public ResponseEntity<List<BannerSummary>> getActiveBanners() {
        List<BannerSummary> banners = bannerRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(b -> new BannerSummary(b.getId(), b.getTitle(), b.getSubtitle(), b.getImageUrl(), b.getLinkUrl()))
                .toList();
        return ResponseEntity.ok(banners);
    }

    public record BannerSummary(Long id, String title, String subtitle, String imageUrl, String linkUrl) {
    }

    public record ServiceCategorySummary(Long id, String code, String name, String description) {
    }

    @GetMapping("/vendor-profiles/by-tenant/{tenantId}")
    public ResponseEntity<VendorProfileSummaryResponse> getByTenant(@PathVariable Long tenantId) {
        VendorProfile profile = vendorProfileRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant chưa có hồ sơ vendor: " + tenantId));
        ServiceCategory category = serviceCategoryRepository.findById(profile.getServiceCategoryId()).orElse(null);
        return ResponseEntity.ok(VendorProfileSummaryResponse.builder()
                .id(profile.getId())
                .tenantId(profile.getTenantId())
                .serviceCategoryId(profile.getServiceCategoryId())
                .serviceCategoryName(category != null ? category.getName() : null)
                .businessName(profile.getBusinessName())
                .active(profile.isActive())
                .build());
    }
}
