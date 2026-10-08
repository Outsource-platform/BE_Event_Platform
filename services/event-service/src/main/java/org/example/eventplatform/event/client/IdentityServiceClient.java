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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IdentityServiceClient {

    private final RestClient restClient;

    public IdentityServiceClient(@Value("${identity-service.base-url}") String baseUrl,
                                  @Value("${internal.service-token:}") String internalToken) {
        this.restClient = InternalRestClients.create(baseUrl, internalToken);
    }

    public TenantSummary findTenant(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/internal/tenants/{tenantId}", tenantId)
                    .retrieve()
                    .body(TenantSummary.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            log.error("Could not fetch tenant {}", tenantId, ex);
            return null;
        } catch (Exception ex) {
            log.error("Could not fetch tenant {}", tenantId, ex);
            return null;
        }
    }

    public UserContact findUser(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/internal/users/{userId}", userId)
                    .retrieve()
                    .body(UserContact.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            log.error("Could not fetch user {}", userId, ex);
            return null;
        } catch (Exception ex) {
            log.error("Could not fetch user {}", userId, ex);
            return null;
        }
    }

    public Map<Long, UserContact> findUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        try {
            UserContact[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/users")
                            .queryParam("ids", userIds.toArray())
                            .build())
                    .retrieve()
                    .body(UserContact[].class);
            if (response == null) {
                return Map.of();
            }
            return Arrays.stream(response)
                    .collect(Collectors.toMap(UserContact::userId, Function.identity(), (a, b) -> a));
        } catch (Exception ex) {
            log.error("Could not fetch users {}", userIds, ex);
            return Map.of();
        }
    }

    /** Danh sách đoàn cho sàn khách hàng. Lỗi thì trả rỗng để trang khám phá không vỡ. */
    public List<PublicTenant> findPublicTenants() {
        try {
            return publicTenantsCache.get(this::fetchPublicTenants);
        } catch (Exception ex) {
            log.error("Could not fetch public tenants", ex);
            return List.of();
        }
    }

    // Mỗi lượt mở trang chủ/duyệt sàn đều cần danh sách này; không cache thì mỗi lần đều đi một vòng sang
    // identity-service và quét bảng tenants. 30 giây là đủ để đơn vị mới/bị khoá hiện ra gần như tức thì.
    private final TtlCache<List<PublicTenant>> publicTenantsCache = new TtlCache<>(Duration.ofSeconds(30));

    private List<PublicTenant> fetchPublicTenants() {
        PublicTenant[] response = restClient.get()
                .uri("/api/internal/tenants/public")
                .retrieve()
                .body(PublicTenant[].class);
        return response == null ? List.of() : List.copyOf(Arrays.asList(response));
    }

    public record TenantSummary(Long id, String name, String domain, String email, boolean active) {
    }

    public record PublicTenant(Long id, String name, String domain, String logo, String category,
                               String province, String ward, String primaryColorHex, String accentColorHex) {
    }

    public record UserContact(Long userId, Long tenantId, String username, String fullName, String email,
                               String availabilityStatus, java.math.BigDecimal commissionRate,
                               String bankName, String bankAccountNumber, String bankAccountHolder) {
    }
}
