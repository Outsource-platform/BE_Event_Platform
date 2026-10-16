package org.example.eventplatform.event.service;

import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.entity.ChatConversation;
import org.example.eventplatform.event.repository.ChatConversationRepository;
import org.example.eventplatform.event.repository.ChatMessageRepository;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.shared.exception.ApiException;
import org.example.eventplatform.event.service.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatBlockTest {

    private final ChatConversationRepository conversations = mock(ChatConversationRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final NotificationPublisher publisher = mock(NotificationPublisher.class);
    private ChatService service;
    private ChatConversation c;

    @BeforeEach
    void setUp() {
        service = new ChatService(conversations, messages, mock(EventRepository.class), mock(IdentityServiceClient.class), publisher);
        // Khách 10 nhắn với đoàn 1, người nhận phía đoàn là user 20.
        c = ChatConversation.builder().id(7L).tenantId(1L).customerUserId(10L).troupeUserId(20L)
                .customerName("Khách").tenantName("Đoàn").build();
        when(conversations.findById(7L)).thenReturn(Optional.of(c));
        when(conversations.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void doanChanThiKhongBenNaoGuiDuocVaKhongCoThongBao() {
        var conv = service.setBlocked(7L, new ChatService.Caller(20L, 1L, false), true);
        assertThat(c.isTroupeBlocked()).isTrue();
        assertThat(conv.blocked()).isTrue();
        assertThat(conv.blockedByMe()).isTrue();
        assertThatThrownBy(() -> service.send(7L, new ChatService.Caller(10L, null, false), "chào", null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.send(7L, new ChatService.Caller(20L, 1L, false), "chào", null))
                .isInstanceOf(ApiException.class);
        verify(messages, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void khachThayBiChanNhungKhongPhaiDoMinhChan() {
        service.setBlocked(7L, new ChatService.Caller(20L, 1L, false), true);
        var conv = service.setBlocked(7L, new ChatService.Caller(10L, null, false), false);
        assertThat(conv.blocked()).isTrue();
        assertThat(conv.blockedByMe()).isFalse();
    }

    @Test
    void boChanThiGuiLaiDuoc() {
        service.setBlocked(7L, new ChatService.Caller(10L, null, false), true);
        assertThat(c.isCustomerBlocked()).isTrue();
        service.setBlocked(7L, new ChatService.Caller(10L, null, false), false);
        when(messages.save(any())).thenAnswer(i -> i.getArgument(0));
        service.send(7L, new ChatService.Caller(10L, null, false), "chào", null);
        verify(messages).save(any());
    }

    @Test
    void nguoiNgoaiKhongChanDuoc() {
        assertThatThrownBy(() -> service.setBlocked(7L, new ChatService.Caller(99L, 2L, true), true))
                .isInstanceOf(RuntimeException.class);
        assertThat(c.isTroupeBlocked()).isFalse();
    }
}
