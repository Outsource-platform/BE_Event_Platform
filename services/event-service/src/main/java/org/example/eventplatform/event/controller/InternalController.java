package org.example.eventplatform.event.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.service.ChatService;
import org.example.eventplatform.event.service.ShowRatingService;
import org.example.eventplatform.event.service.WithdrawalService;
import org.example.eventplatform.event.entity.WithdrawalStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chỉ service khác gọi (không có đường qua gateway), bảo vệ bằng X-Internal-Token. */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final ChatService chatService;
    private final ShowRatingService ratingService;
    private final org.example.eventplatform.event.repository.UserEventRepository userEventRepository;
    private final org.example.eventplatform.event.repository.EventRepository eventRepository;
    private final WithdrawalService withdrawalService;
    private final org.example.eventplatform.event.repository.WithdrawalRequestRepository withdrawalRequestRepository;

    /** Tình trạng còn dở của một thành viên, để trưởng đoàn biết có xoá được không. */
    @GetMapping("/members/{userId}/delete-check")
    public ResponseEntity<MemberDeleteCheck> memberDeleteCheck(@PathVariable Long userId, @RequestParam Long tenantId) {
        return ResponseEntity.ok(new MemberDeleteCheck(
                userEventRepository.countOpenAssignments(userId),
                eventRepository.countOpenContracts(tenantId, userId),
                withdrawalService.availableBalance(tenantId, userId),
                withdrawalRequestRepository.countByUserIdAndStatus(userId, WithdrawalStatus.PENDING)));
    }

    public record MemberDeleteCheck(long openAssignments, long openContracts, java.math.BigDecimal balance, long pendingWithdrawals) {
    }

    /** Dọn dữ liệu của một khách đã xoá tài khoản: tin nhắn bị xoá, đánh giá được ẩn danh. */
    @DeleteMapping("/users/{id}/data")
    public ResponseEntity<Void> deleteUserData(@PathVariable Long id) {
        chatService.deleteCustomerData(id);
        ratingService.anonymizeUser(id);
        return ResponseEntity.noContent().build();
    }
}
