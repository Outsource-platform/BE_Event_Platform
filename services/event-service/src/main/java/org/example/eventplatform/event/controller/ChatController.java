package org.example.eventplatform.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.ChatDtos;
import org.example.eventplatform.event.service.ChatService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Nhắn tin khách ↔ trưởng đoàn. Khách dùng /api/customer/chat, trưởng đoàn dùng /api/tenant/chat. */
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ===== Khách =====

    @PostMapping("/api/customer/chat/conversations")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ChatDtos.Conversation> open(@AuthenticationPrincipal JwtPrincipal principal,
                                                      @RequestBody ChatDtos.OpenRequest request) {
        return ResponseEntity.ok(chatService.open(principal.userId(), request.getTenantId(), request.getEventId()));
    }

    @GetMapping("/api/customer/chat/conversations")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ChatDtos.Inbox> customerInbox(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(chatService.inboxForCustomer(principal.userId()));
    }

    @GetMapping("/api/customer/chat/conversations/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ChatDtos.Thread> customerThread(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id,
                                                          @RequestParam(required = false) Long after) {
        return ResponseEntity.ok(chatService.thread(id, principal.userId(), false, after));
    }

    @PostMapping("/api/customer/chat/conversations/{id}/messages")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ChatDtos.Message> customerSend(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id,
                                                         @Valid @RequestBody ChatDtos.SendRequest request) {
        return ResponseEntity.ok(chatService.send(id, principal.userId(), false, principal.userId(), request.getContent(), request.getImageUrl()));
    }

    // ===== Trưởng đoàn =====

    @GetMapping("/api/tenant/chat/conversations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChatDtos.Inbox> tenantInbox(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(chatService.inboxForTenant(principal.tenantId()));
    }

    @GetMapping("/api/tenant/chat/conversations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChatDtos.Thread> tenantThread(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id,
                                                        @RequestParam(required = false) Long after) {
        return ResponseEntity.ok(chatService.thread(id, principal.tenantId(), true, after));
    }

    @PostMapping("/api/tenant/chat/conversations/{id}/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChatDtos.Message> tenantSend(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id,
                                                       @Valid @RequestBody ChatDtos.SendRequest request) {
        return ResponseEntity.ok(chatService.send(id, principal.tenantId(), true, principal.userId(), request.getContent(), request.getImageUrl()));
    }
}
