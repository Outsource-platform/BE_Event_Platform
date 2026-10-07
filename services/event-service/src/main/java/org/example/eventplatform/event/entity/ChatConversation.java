package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * Cuộc trò chuyện giữa một khách và một đoàn, gắn với show khách đang xem (nếu có). Tên khách, tên đoàn và
 * tiêu đề show được chụp lại lúc mở để danh sách tin nhắn hiện được mà không phải gọi sang dịch vụ khác.
 */
@Entity
@Table(name = "chat_conversations", indexes = {
        @Index(name = "idx_chat_tenant", columnList = "tenant_id, last_message_at"),
        @Index(name = "idx_chat_customer", columnList = "customer_user_id, last_message_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "event_id")
    private Long eventId;

    private String customerName;
    private String tenantName;
    private String eventTitle;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(length = 200)
    private String lastMessagePreview;

    @Builder.Default
    private int tenantUnread = 0;

    @Builder.Default
    private int customerUnread = 0;
}
