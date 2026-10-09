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
 * Nhắn tin: khách ↔ người đăng show (hoặc trưởng đoàn), thành viên ↔ trưởng đoàn. Cuộc trò chuyện có một bên mở
 * ([customerUserId]) và một bên đoàn (người nhận cụ thể, hoặc trưởng đoàn nào của đoàn cũng được).
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

    /** Người đang gọi: [tenantId] có với trưởng đoàn và thành viên, [admin] là trưởng đoàn. */
    public record Caller(Long userId, Long tenantId, boolean admin) {
    }

    /**
     * Mở (hoặc tạo) cuộc trò chuyện.
     * - Khách: nhắn một đoàn, gắn với show nếu đang xem show; người nhận là người đã đăng show đó lên Khám phá.
     * - Thành viên: nhắn trưởng đoàn của đoàn mình.
     */
    @Transactional
    public ChatDtos.Conversation open(Caller caller, Long tenantId, Long eventId) {
        boolean troupe = caller.tenantId() != null;
        if (troupe && caller.admin()) {
            throw new IllegalArgumentException("Trưởng đoàn nhận tin từ khách và thành viên, không tự mở cuộc trò chuyện");
        }
        Long targetTenant = troupe ? caller.tenantId() : tenantId;
        if (targetTenant == null) {
            throw new IllegalArgumentException("Thiếu đoàn cần nhắn tin");
        }
        if (troupe && eventId != null) {
            throw new IllegalArgumentException("Thành viên nhắn trưởng đoàn, không gắn với show của khách");
        }
        var tenant = identityServiceClient.findTenant(targetTenant);
        if (tenant == null || !tenant.active()) {
            throw new IllegalArgumentException("Không tìm thấy đoàn này");
        }
        Event event = null;
        if (eventId != null) {
            event = eventRepository.findById(eventId)
                    .filter(e -> targetTenant.equals(e.getTenantId()))
                    .orElseThrow(() -> new IllegalArgumentException("Show không thuộc đoàn này"));
        }
        var existing = event == null
                ? conversationRepository.findFirstByTenantIdAndCustomerUserIdAndEventIdIsNull(targetTenant, caller.userId())
                : conversationRepository.findFirstByTenantIdAndCustomerUserIdAndEventId(targetTenant, caller.userId(), eventId);
        final Event shown = event;
        ChatConversation conversation = existing.orElseGet(() -> {
            var user = identityServiceClient.findUser(caller.userId());
            var recipient = recipientOf(shown, targetTenant, caller.userId());
            return conversationRepository.save(ChatConversation.builder()
                    .tenantId(targetTenant).customerUserId(caller.userId()).eventId(eventId)
                    .troupeUserId(recipient == null ? null : recipient.userId())
                    .troupeUserName(recipient == null ? null : recipient.fullName())
                    .customerName(user != null && user.fullName() != null ? user.fullName() : "Khách")
                    .tenantName(tenant.name())
                    .eventTitle(titleOf(shown))
                    .lastMessageAt(Clocks.utcNow())
                    .build());
        });
        return toConversation(conversation, caller.userId());
    }

    /** Người đăng show lên Khám phá nhận tin; nếu người đó là trưởng đoàn thì để trống để trưởng đoàn nào cũng trả lời được. */
    private IdentityServiceClient.UserContact recipientOf(Event event, Long tenantId, Long callerId) {
        if (event == null || event.getShowcasePublishedBy() == null || event.getShowcasePublishedBy().equals(callerId)) {
            return null;
        }
        var publisher = identityServiceClient.findUser(event.getShowcasePublishedBy());
        if (publisher == null || !tenantId.equals(publisher.tenantId()) || "ADMIN".equals(publisher.roleName())) {
            return null;
        }
        return publisher;
    }

    @Transactional(readOnly = true)
    public ChatDtos.Inbox inbox(Caller caller) {
        var list = conversationRepository.findInbox(caller.userId(), caller.admin(), caller.tenantId());
        int total = list.stream().mapToInt(c -> unreadFor(c, caller.userId())).sum();
        return new ChatDtos.Inbox(total, list.stream().map(c -> toConversation(c, caller.userId())).toList());
    }

    /** Tin nhắn mới hơn [afterId]; gọi là tính đã đọc phía người gọi. */
    @Transactional
    public ChatDtos.Thread thread(Long conversationId, Caller caller, Long afterId) {
        ChatConversation c = authorized(conversationId, caller);
        boolean asTenant = isTroupeSide(c, caller.userId());
        List<ChatMessage> messages = messageRepository
                .findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, afterId == null ? 0L : afterId, PageRequest.of(0, PAGE));
        if (unreadFor(c, caller.userId()) > 0) {
            if (asTenant) {
                c.setTenantUnread(0);
            } else {
                c.setCustomerUnread(0);
            }
            conversationRepository.save(c);
        }
        return new ChatDtos.Thread(toConversation(c, caller.userId()), messages.stream().map(ChatService::toMessage).toList());
    }

    @Transactional
    public ChatDtos.Message send(Long conversationId, Caller caller, String content, String imageUrl) {
        ChatConversation c = authorized(conversationId, caller);
        boolean asTenant = isTroupeSide(c, caller.userId());
        String text = content == null ? "" : content.trim();
        String image = imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
        if (text.isEmpty() && image == null) {
            throw new IllegalArgumentException("Nhập nội dung tin nhắn hoặc chọn ảnh");
        }
        if (image != null && imagePrefixes.stream().noneMatch(p -> !p.isBlank() && image.startsWith(p.trim()))) {
            throw new IllegalArgumentException("Ảnh không hợp lệ, hãy tải ảnh lên bằng ứng dụng");
        }
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .conversationId(conversationId).sender(asTenant ? "TROUPE" : "CUSTOMER").senderUserId(caller.userId())
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
        Map<String, String> data = Map.of("conversationId", String.valueOf(c.getId()));
        if (asTenant) {
            notificationPublisher.publish("CHAT_MESSAGE", c.getCustomerUserId(), null, troupeLabel(c) + " đã trả lời",
                    c.getLastMessagePreview(), data);
        } else if (c.getTroupeUserId() != null) {
            notificationPublisher.publish("CHAT_MESSAGE", c.getTroupeUserId(), null, c.getCustomerName() + " nhắn tin" + about,
                    c.getLastMessagePreview(), data);
        } else {
            notificationPublisher.publish("CHAT_MESSAGE", null, c.getTenantId(), c.getCustomerName() + " nhắn tin" + about,
                    c.getLastMessagePreview(), data);
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

    /** Phía đoàn = không phải bên đã mở cuộc trò chuyện. */
    private static boolean isTroupeSide(ChatConversation c, Long userId) {
        return !c.getCustomerUserId().equals(userId);
    }

    private static int unreadFor(ChatConversation c, Long userId) {
        return isTroupeSide(c, userId) ? c.getTenantUnread() : c.getCustomerUnread();
    }

    private ChatConversation authorized(Long conversationId, Caller caller) {
        ChatConversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc trò chuyện"));
        boolean ok = c.getCustomerUserId().equals(caller.userId())
                || caller.userId().equals(c.getTroupeUserId())
                || (caller.admin() && c.getTroupeUserId() == null && c.getTenantId().equals(caller.tenantId()));
        if (!ok) {
            throw new AccessDeniedException("Bạn không có quyền xem cuộc trò chuyện này");
        }
        return c;
    }

    private static String troupeLabel(ChatConversation c) {
        return c.getTroupeUserName() == null ? c.getTenantName() : c.getTroupeUserName() + " · " + c.getTenantName();
    }

    private static ChatDtos.Conversation toConversation(ChatConversation c, Long viewerId) {
        boolean asTenant = isTroupeSide(c, viewerId);
        return new ChatDtos.Conversation(c.getId(), c.getTenantId(), c.getTenantName(), c.getCustomerName(), c.getEventId(),
                c.getEventTitle(), c.getLastMessagePreview(), c.getLastMessageAt(), unreadFor(c, viewerId),
                asTenant ? c.getCustomerName() : troupeLabel(c));
    }

    private static ChatDtos.Message toMessage(ChatMessage m) {
        return new ChatDtos.Message(m.getId(), m.getSender(), m.getSenderUserId(), m.getContent(), m.getImageUrl(), m.getCreatedAt());
    }

    private static String titleOf(Event event) {
        if (event == null) {
            return null;
        }
        return event.getShowcaseTitle() != null ? event.getShowcaseTitle() : event.getName();
    }
}
