package org.example.eventplatform.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.ChatDtos;
import org.example.eventplatform.event.service.ChatService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Nhắn tin chung cho khách, thành viên và trưởng đoàn; phía nào của cuộc trò chuyện thì tự suy ra từ người gọi. */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private static ChatService.Caller caller(JwtPrincipal principal) {
        return new ChatService.Caller(principal.userId(), principal.tenantId(), principal.authorities().contains("ROLE_ADMIN"));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ChatDtos.Conversation> open(@AuthenticationPrincipal JwtPrincipal principal,
                                                      @RequestBody ChatDtos.OpenRequest request) {
        return ResponseEntity.ok(chatService.open(caller(principal), request.getTenantId(), request.getEventId()));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ChatDtos.Inbox> inbox(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(chatService.inbox(caller(principal)));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ChatDtos.Thread> thread(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id,
                                                  @RequestParam(required = false) Long after) {
        return ResponseEntity.ok(chatService.thread(id, caller(principal), after));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ChatDtos.Message> send(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id,
                                                 @Valid @RequestBody ChatDtos.SendRequest request) {
        return ResponseEntity.ok(chatService.send(id, caller(principal), request.getContent(), request.getImageUrl()));
    }
}
