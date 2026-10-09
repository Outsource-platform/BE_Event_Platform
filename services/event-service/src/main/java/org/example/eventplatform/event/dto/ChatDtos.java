package org.example.eventplatform.event.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    @Getter
    @Setter
    public static class OpenRequest {
        private Long tenantId;
        private Long eventId;
    }

    @Getter
    @Setter
    public static class SendRequest {
        // Một tin phải có chữ hoặc ảnh (kiểm tra ở ChatService).
        @Size(max = 2000, message = "Tin nhắn tối đa 2000 ký tự")
        private String content;

        @Size(max = 600, message = "Đường dẫn ảnh quá dài")
        private String imageUrl;
    }

    /** Một dòng trong danh sách hội thoại; [unread] là số tin chưa đọc của bên đang xem. */
    public record Conversation(Long id, Long tenantId, String tenantName, String customerName, Long eventId,
                               String eventTitle, String lastMessage, LocalDateTime lastMessageAt, int unread) {
    }

    public record Message(Long id, String sender, String content, String imageUrl, LocalDateTime sentAt) {
    }

    public record Thread(Conversation conversation, List<Message> messages) {
    }

    public record Inbox(int totalUnread, List<Conversation> conversations) {
    }
}
