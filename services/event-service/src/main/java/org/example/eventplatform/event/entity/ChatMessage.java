package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_msg_conv", columnList = "conversation_id, id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    // CUSTOMER hoặc TROUPE: bên nào gửi.
    @Column(nullable = false, length = 10)
    private String sender;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Column(nullable = false, length = 2000)
    private String content;
}
