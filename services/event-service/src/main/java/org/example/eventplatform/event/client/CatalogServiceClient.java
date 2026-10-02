package org.example.eventplatform.event.client;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.shared.client.InternalRestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class CatalogServiceClient {

    private final RestClient restClient;

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
            ServiceCategorySummary[] response = restClient.get()
                    .uri("/api/internal/service-categories")
                    .retrieve()
                    .body(ServiceCategorySummary[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (Exception ex) {
            log.error("Could not fetch service categories", ex);
            return List.of();
        }
    }

    /** Banner trang chủ đang bật. Lỗi thì trả rỗng để home không vỡ. */
    public List<BannerSummary> listBanners() {
        try {
            BannerSummary[] response = restClient.get()
                    .uri("/api/internal/banners")
                    .retrieve()
                    .body(BannerSummary[].class);
            return response == null ? List.of() : Arrays.asList(response);
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
