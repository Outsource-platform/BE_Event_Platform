package org.example.eventplatform.customer.client;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.shared.client.InternalRestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tra tên và đơn vị của người dùng để hiện "người phụ trách" và kiểm tra người được gán thuộc cùng đoàn. */
@Component
@Slf4j
public class IdentityServiceClient {

    private final RestClient restClient;

    public IdentityServiceClient(@Value("${identity-service.base-url}") String baseUrl,
                                 @Value("${internal.service-token:}") String internalToken) {
        this.restClient = InternalRestClients.create(baseUrl, internalToken);
    }

    public record UserInfo(Long userId, Long tenantId, String fullName, String username, String roleName) {
        public String displayName() {
            return fullName != null && !fullName.isBlank() ? fullName : username;
        }
    }

    /** Lỗi gọi dịch vụ thì trả rỗng: danh sách khách vẫn xem được, chỉ thiếu tên người phụ trách. */
    public Map<Long, UserInfo> findUsers(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        try {
            UserInfo[] response = restClient.get()
                    .uri(b -> b.path("/api/internal/users").queryParam("ids", ids.toArray()).build())
                    .retrieve()
                    .body(UserInfo[].class);
            if (response == null) {
                return Map.of();
            }
            return Arrays.stream(response).collect(Collectors.toMap(UserInfo::userId, Function.identity(), (a, b) -> a));
        } catch (Exception ex) {
            log.error("Could not fetch users {}", ids, ex);
            return Map.of();
        }
    }
}
