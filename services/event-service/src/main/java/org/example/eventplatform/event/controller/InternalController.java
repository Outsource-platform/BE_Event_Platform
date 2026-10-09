package org.example.eventplatform.event.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.service.ChatService;
import org.example.eventplatform.event.service.ShowRatingService;
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

    /** Dọn dữ liệu của một khách đã xoá tài khoản: tin nhắn bị xoá, đánh giá được ẩn danh. */
    @DeleteMapping("/users/{id}/data")
    public ResponseEntity<Void> deleteUserData(@PathVariable Long id) {
        chatService.deleteCustomerData(id);
        ratingService.anonymizeUser(id);
        return ResponseEntity.noContent().build();
    }
}
