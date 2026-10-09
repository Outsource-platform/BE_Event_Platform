package org.example.eventplatform.event.service;

import org.example.eventplatform.shared.time.Clocks;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.dto.ChatDtos;
import org.example.eventplatform.event.entity.ChatConversation;
import org.example.eventplatform.event.entity.ChatMessage;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.repository.ChatConversationRepository;
import org.example.eventplatform.event.repository.ChatMessageRepository;
import org.example.eventplatform.event.repository.EventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Nhắn tin giữa khách và trưởng đoàn, gắn với show khách đang xem. Hai phía dùng chung một bộ hàm;
 * [asTenant] cho biết người gọi là đoàn (quản trị của tenantId) hay là khách (customerUserId).
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int PAGE = 100;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final EventRepository eventRepository;
    private final IdentityServiceClient identityServiceClient;
    private final NotificationPublisher notificationPublisher;

    // Chỉ nhận ảnh nằm trên kho của hệ thống, tránh người dùng chèn đường dẫn ngoài (theo dõi, nội dung lạ) vào cuộc trò chuyện.
    @org.springframework.beans.factory.annotation.Value("${chat.image-prefixes:https://muong14.xyz/}")
    private List<String> imagePrefixes;

    // ===== Khách =====

    /** Mở (hoặc tạo) hội thoại với một đoàn, gắn với show nếu khách đang xem một show. */
    @Transactional
    public ChatDtos.Conversation open(Long customerUserId, Long tenantId, Long eventId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Thiếu đoàn cần nhắn tin");
        }
        var tenant = identityServiceClient.findTenant(tenantId);
        if (tenant == null || !tenant.active()) {
            throw new IllegalArgumentException("Không tìm thấy đoàn này");
        }
        Event event = null;
        if (eventId != null) {
            event = eventRepository.findById(eventId)
                    .filter(e -> tenantId.equals(e.getTenantId()))
                    .orElseThrow(() -> new IllegalArgumentException("Show không thuộc đoàn này"));
        }
        var existing = event == null
                ? conversationRepository.findFirstByTenantIdAndCustomerUserIdAndEventIdIsNull(tenantId, customerUserId)
                : conversationRepository.findFirstByTenantIdAndCustomerUserIdAndEventId(tenantId, customerUserId, eventId);
        ChatConversation conversation = existing.orElseGet(() -> {
            var user = identityServiceClient.findUser(customerUserId);
            return conversationRepository.save(ChatConversation.builder()
                    .tenantId(tenantId).customerUserId(customerUserId).eventId(eventId)
                    .customerName(user != null && user.fullName() != null ? user.fullName() : "Khách")
                    .tenantName(tenant.name())
                    .eventTitle(titleOf(eventRepository.findById(eventId == null ? -1L : eventId).orElse(null)))
                    .lastMessageAt(Clocks.utcNow())
                    .build());
        });
        return toConversation(conversation, false);
    }

    @Transactional(readOnly = true)
    public ChatDtos.Inbox inboxForCustomer(Long customerUserId) {
        return inbox(conversationRepository.findByCustomerUserIdOrderByLastMessageAtDesc(customerUserId), false);
    }

    // ===== Đoàn =====

    @Transactional(readOnly = true)
    public ChatDtos.Inbox inboxForTenant(Long tenantId) {
        return inbox(conversationRepository.findByTenantIdOrderByLastMessageAtDesc(tenantId), true);
    }

    // ===== Chung =====

    /** Tin nhắn mới hơn [afterId]; gọi là tính đã đọc phía người gọi. */
    @Transactional
    public ChatDtos.Thread thread(Long conversationId, Long callerId, boolean asTenant, Long afterId) {
        ChatConversation c = authorized(conversationId, callerId, asTenant);
        List<ChatMessage> messages = messageRepository
                .findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, afterId == null ? 0L : afterId, PageRequest.of(0, PAGE));
        boolean changed = asTenant ? c.getTenantUnread() > 0 : c.getCustomerUnread() > 0;
        if (changed) {
            if (asTenant) {
                c.setTenantUnread(0);
            } else {
                c.setCustomerUnread(0);
            }
            conversationRepository.save(c);
        }
        return new ChatDtos.Thread(toConversation(c, asTenant), messages.stream().map(ChatService::toMessage).toList());
    }

    @Transactional
    public ChatDtos.Message send(Long conversationId, Long callerId, boolean asTenant, Long senderUserId, String content, String imageUrl) {
        ChatConversation c = authorized(conversationId, callerId, asTenant);
        String text = content == null ? "" : content.trim();
        String image = imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
        if (text.isEmpty() && image == null) {
            throw new IllegalArgumentException("Nhập nội dung tin nhắn hoặc chọn ảnh");
        }
        if (image != null && imagePrefixes.stream().noneMatch(p -> !p.isBlank() && image.startsWith(p.trim()))) {
            throw new IllegalArgumentException("Ảnh không hợp lệ, hãy tải ảnh lên bằng ứng dụng");
        }
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .conversationId(conversationId).sender(asTenant ? "TROUPE" : "CUSTOMER").senderUserId(senderUserId)
                .content(text).imageUrl(image).build());
        String preview = text.isEmpty() ? "[Ảnh]" : (image != null ? "[Ảnh] " + text : text);
        c.setLastMessageAt(Clocks.utcNow());
        c.setLastMessagePreview(preview.length() > 120 ? preview.substring(0, 117) + "..." : preview);
        if (asTenant) {
            c.setCustomerUnread(c.getCustomerUnread() + 1);
        } else {
            c.setTenantUnread(c.getTenantUnread() + 1);
        }
        conversationRepository.save(c);

        String about = c.getEventTitle() != null ? " về \"" + c.getEventTitle() + "\"" : "";
        if (asTenant) {
            notificationPublisher.publish("CHAT_MESSAGE", c.getCustomerUserId(), null, c.getTenantName() + " đã trả lời",
                    c.getLastMessagePreview(), Map.of("conversationId", String.valueOf(c.getId())));
        } else {
            notificationPublisher.publish("CHAT_MESSAGE", null, c.getTenantId(), c.getCustomerName() + " nhắn tin" + about,
                    c.getLastMessagePreview(), Map.of("conversationId", String.valueOf(c.getId())));
        }
        return toMessage(saved);
    }

    /** Khách xoá tài khoản: xoá các cuộc trò chuyện và tin nhắn của khách đó (tin nhắn có thể chứa thông tin cá nhân). */
    @Transactional
    public void deleteCustomerData(Long customerUserId) {
        for (ChatConversation c : conversationRepository.findByCustomerUserIdOrderByLastMessageAtDesc(customerUserId)) {
            messageRepository.deleteByConversationId(c.getId());
            conversationRepository.delete(c);
        }
    }

    // ===== Nội bộ =====

    private ChatConversation authorized(Long conversationId, Long callerId, boolean asTenant) {
        ChatConversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));
        boolean ok = asTenant ? c.getTenantId().equals(callerId) : c.getCustomerUserId().equals(callerId);
        if (!ok) {
            throw new AccessDeniedException("Bạn không có quyền xem cuộc trò chuyện này");
        }
        return c;
    }

    private ChatDtos.Inbox inbox(List<ChatConversation> list, boolean asTenant) {
        int total = list.stream().mapToInt(c -> asTenant ? c.getTenantUnread() : c.getCustomerUnread()).sum();
        return new ChatDtos.Inbox(total, list.stream().map(c -> toConversation(c, asTenant)).toList());
    }

    private static ChatDtos.Conversation toConversation(ChatConversation c, boolean asTenant) {
        return new ChatDtos.Conversation(c.getId(), c.getTenantId(), c.getTenantName(), c.getCustomerName(), c.getEventId(),
                c.getEventTitle(), c.getLastMessagePreview(), c.getLastMessageAt(),
                asTenant ? c.getTenantUnread() : c.getCustomerUnread());
    }

    private static ChatDtos.Message toMessage(ChatMessage m) {
        return new ChatDtos.Message(m.getId(), m.getSender(), m.getContent(), m.getImageUrl(), m.getCreatedAt());
    }

    private static String titleOf(Event event) {
        if (event == null) {
            return null;
        }
        return event.getShowcaseTitle() != null ? event.getShowcaseTitle() : event.getName();
    }
}
