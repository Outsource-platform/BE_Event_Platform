package org.example.eventplatform.event.client;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.shared.client.InternalRestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CustomerServiceClient {

    private final RestClient restClient;

    public CustomerServiceClient(@Value("${customer-service.base-url}") String baseUrl,
                                  @Value("${internal.service-token:}") String internalToken) {
        this.restClient = InternalRestClients.create(baseUrl, internalToken);
    }

    public CustomerSummary findCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/internal/customers/{id}", customerId)
                    .retrieve()
                    .body(CustomerSummary.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            log.error("Could not fetch customer {}", customerId, ex);
            return null;
        } catch (Exception ex) {
            log.error("Could not fetch customer {}", customerId, ex);
            return null;
        }
    }

    /**
     * Strict lookup for write paths — throws when the customer is missing or the
     * remote call fails, so we never persist a dangling customerId.
     */
    public CustomerSummary requireCustomer(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId không được để trống");
        }
        try {
            return restClient.get()
                    .uri("/api/internal/customers/{id}", customerId)
                    .retrieve()
                    .body(CustomerSummary.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng với ID: " + customerId);
            }
            throw new IllegalStateException("Không gọi được customer-service để kiểm tra khách hàng", ex);
        }
    }

    /** Find-or-create CRM row for marketplace booking; always throws on remote failure. */
    public CustomerSummary findOrCreate(Long tenantId, String phone, Long userId, String fullName, String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("tenant_id", tenantId);
        body.put("phone", phone);
        body.put("user_id", userId);
        body.put("full_name", fullName);
        body.put("email", email);
        try {
            CustomerSummary created = restClient.post()
                    .uri("/api/internal/customers/find-or-create")
                    .body(body)
                    .retrieve()
                    .body(CustomerSummary.class);
            if (created == null || created.id() == null) {
                throw new IllegalStateException("customer-service không trả về khách hàng");
            }
            return created;
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Không tạo được hồ sơ khách trên customer-service", ex);
        }
    }

    /**
     * Tên khách theo id, tra một lần cho cả trang danh sách. Lỗi thì trả map rỗng (tên khách để trống) chứ không
     * làm hỏng cả danh sách show.
     */
    public java.util.Map<Long, String> findNamesByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Map.of();
        }
        try {
            CustomerSummary[] response = restClient.post()
                    .uri("/api/internal/customers/by-ids")
                    .body(java.util.Map.of("ids", ids))
                    .retrieve()
                    .body(CustomerSummary[].class);
            java.util.Map<Long, String> names = new java.util.HashMap<>();
            if (response != null) {
                for (CustomerSummary c : response) {
                    names.put(c.id(), c.fullName());
                }
            }
            return names;
        } catch (Exception ex) {
            log.error("Could not batch-fetch customers {}", ids, ex);
            return java.util.Map.of();
        }
    }

    public List<CustomerSummary> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            CustomerSummary[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/customers")
                            .queryParam("userId", userId)
                            .build())
                    .retrieve()
                    .body(CustomerSummary[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (Exception ex) {
            log.error("Could not list customers for user {}", userId, ex);
            return List.of();
        }
    }

    public record CustomerSummary(Long id, Long tenantId, String fullName, String phone, String email, Long userId) {
    }
}
