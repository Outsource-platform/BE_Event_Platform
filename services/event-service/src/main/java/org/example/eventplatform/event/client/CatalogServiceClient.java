package org.example.eventplatform.event.client;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.shared.cache.TtlCache;
import org.example.eventplatform.shared.client.InternalRestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class CatalogServiceClient {

    private final RestClient restClient;

    // Danh mục và banner ít đổi nhưng ai mở trang chủ cũng cần: cache ngắn để không gọi sang catalog-service mỗi lần.
    private final TtlCache<List<ServiceCategorySummary>> categoriesCache = new TtlCache<>(Duration.ofSeconds(60));
    private final TtlCache<List<BannerSummary>> bannersCache = new TtlCache<>(Duration.ofSeconds(30));

    public CatalogServiceClient(@Value("${catalog-service.base-url}") String baseUrl,
                                 @Value("${internal.service-token:}") String internalToken) {
        this.restClient = InternalRestClients.create(baseUrl, internalToken);
    }

    public VendorProfileSummary findVendorByTenant(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/internal/vendor-profiles/by-tenant/{tenantId}", tenantId)
                    .retrieve()
                    .body(VendorProfileSummary.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            log.error("Could not fetch vendor profile for tenant {}", tenantId, ex);
            return null;
        } catch (Exception ex) {
            log.error("Could not fetch vendor profile for tenant {}", tenantId, ex);
            return null;
        }
    }

    /** Danh mục dịch vụ cho trang chủ sàn. Lỗi thì trả rỗng để home không vỡ. */
    public List<ServiceCategorySummary> listServiceCategories() {
        try {
            return categoriesCache.get(() -> {
                ServiceCategorySummary[] response = restClient.get()
                        .uri("/api/internal/service-categories")
                        .retrieve()
                        .body(ServiceCategorySummary[].class);
                return response == null ? List.<ServiceCategorySummary>of() : List.copyOf(Arrays.asList(response));
            });
        } catch (Exception ex) {
            log.error("Could not fetch service categories", ex);
            return List.of();
        }
    }

    /** Banner trang chủ đang bật. Lỗi thì trả rỗng để home không vỡ. */
    public List<BannerSummary> listBanners() {
        try {
            return bannersCache.get(() -> {
                BannerSummary[] response = restClient.get()
                        .uri("/api/internal/banners")
                        .retrieve()
                        .body(BannerSummary[].class);
                return response == null ? List.<BannerSummary>of() : List.copyOf(Arrays.asList(response));
            });
        } catch (Exception ex) {
            log.error("Could not fetch banners", ex);
            return List.of();
        }
    }

    public record BannerSummary(Long id, String title, String subtitle, String imageUrl, String linkUrl) {
    }

    public record ServiceCategorySummary(Long id, String code, String name, String description) {
    }

    public record VendorProfileSummary(
            Long id,
            Long tenantId,
            Long serviceCategoryId,
            String serviceCategoryName,
            String businessName,
            boolean active
    ) {
    }
}
