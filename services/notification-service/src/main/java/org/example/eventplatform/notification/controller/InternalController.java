package org.example.eventplatform.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.notification.repository.FcmTokenRepository;
import org.example.eventplatform.notification.repository.UserNotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only — guarded by {@code X-Internal-Token}. */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalController {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserNotificationRepository userNotificationRepository;

    /** Xoá mã thiết bị và hộp thư thông báo của một người dùng khi họ xoá tài khoản. */
    @DeleteMapping("/users/{userId}/data")
    @Transactional
    public ResponseEntity<Void> deleteUserData(@PathVariable Long userId) {
        int tokens = fcmTokenRepository.deleteAllByUserId(userId);
        int inbox = userNotificationRepository.deleteAllByUserId(userId);
        log.info("Đã xoá dữ liệu thông báo của user {}: {} token, {} thông báo", userId, tokens, inbox);
        return ResponseEntity.noContent().build();
    }
}
