package org.example.eventplatform.notification.client;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.shared.client.InternalRestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public List<AdminContact> getTenantAdmins(Long tenantId) {
        try {
            AdminContact[] response = restClient.get()
                    .uri("/api/internal/tenants/{tenantId}/admins", tenantId)
                    .retrieve()
                    .body(AdminContact[].class);
            return response != null ? List.of(response) : List.of();
        } catch (Exception ex) {
            log.error("Could not fetch admins for tenant {}", tenantId, ex);
            return List.of();
        }
    }

    /** Họ tên để thay "User #id" trong hộp thư đã lưu. Lỗi thì trả rỗng, câu cũ giữ nguyên. */
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

    public record UserContact(Long userId, Long tenantId, String username, String fullName, String email,
                               String availabilityStatus, java.math.BigDecimal commissionRate,
                               String bankName, String bankAccountNumber, String bankAccountHolder) {
    }
}
