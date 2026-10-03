package org.example.eventplatform.catalog.client;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.shared.client.InternalRestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Tra tên/mã đơn vị ở identity-service để ghi vào bài viết (tác giả hiển thị công khai). */
@Component
@Slf4j
public class IdentityClient {

    private final RestClient restClient;

    public IdentityClient(@Value("${identity-service.base-url}") String baseUrl,
                          @Value("${internal.service-token:}") String internalToken) {
        this.restClient = InternalRestClients.create(baseUrl, internalToken);
    }

    /** Lỗi thì trả null để việc đăng bài không bị chặn vì không tra được tên. */
    public TenantSummary findTenant(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            return restClient.get().uri("/api/internal/tenants/{id}", tenantId).retrieve().body(TenantSummary.class);
        } catch (Exception ex) {
            log.warn("Không tra được đơn vị {}", tenantId, ex);
            return null;
        }
    }

    public record TenantSummary(Long id, String name, String domain) {
    }
}
