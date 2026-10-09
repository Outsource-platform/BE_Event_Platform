package org.example.eventplatform.identity.client;

import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.shared.client.InternalRestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Dọn dữ liệu của một người dùng ở các service khác khi họ xoá tài khoản. Khác các client tra cứu khác,
 * lỗi ở đây được ném ra để việc xoá bị huỷ và người dùng thử lại: bỏ sót sẽ để lại dữ liệu cá nhân.
 */
@Component
@Slf4j
public class AccountCleanupClient {

    private final RestClient customerClient;
    private final RestClient notificationClient;
    private final RestClient eventClient;

    public AccountCleanupClient(@Value("${customer-service.base-url}") String customerUrl,
                                @Value("${notification-service.base-url}") String notificationUrl,
                                @Value("${event-service.base-url}") String eventUrl,
                                @Value("${internal.service-token:}") String internalToken) {
        this.customerClient = InternalRestClients.create(customerUrl, internalToken);
        this.notificationClient = InternalRestClients.create(notificationUrl, internalToken);
        this.eventClient = InternalRestClients.create(eventUrl, internalToken);
    }

    public void anonymizeCustomerRecords(Long userId) {
        customerClient.post().uri("/api/internal/customers/anonymize?userId={id}", userId).retrieve().toBodilessEntity();
    }

    /** Xoá tin nhắn và ẩn danh đánh giá của khách ở event-service. */
    public void deleteEventData(Long userId) {
        eventClient.delete().uri("/api/internal/users/{id}/data", userId).retrieve().toBodilessEntity();
    }

    public void deleteNotificationData(Long userId) {
        notificationClient.delete().uri("/api/internal/users/{id}/data", userId).retrieve().toBodilessEntity();
    }
}
