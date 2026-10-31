package org.example.eventplatform.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {

    private final Optional<FirebaseApp> firebaseApp;
    private final FcmTokenService fcmTokenService;

    public void sendPush(String token, String title, String body, Map<String, String> data) {
        if (firebaseApp.isEmpty()) {
            log.info("[DEV] would send FCM push to token={} title=\"{}\" body=\"{}\" data={}", token, title, body, data);
            fcmTokenService.touchToken(token);
            return;
        }

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                // Ưu tiên cao để máy đang ngủ hoặc app đã tắt vẫn hiện ngay, không đợi tới lần đánh thức sau.
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder().setChannelId("high_importance_channel").build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", "10")
                        .setAps(Aps.builder().setSound("default").build())
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance(firebaseApp.get()).send(message);
            log.info("Sent FCM push, response={}", response);
            fcmTokenService.touchToken(token);
        } catch (FirebaseMessagingException ex) {
            if (isInvalidToken(ex)) {
                log.warn("FCM token invalid ({}), removing", ex.getMessagingErrorCode());
                fcmTokenService.removeInvalidToken(token);
            } else {
                log.error("Failed to send FCM push to token={}", token, ex);
            }
        } catch (Exception ex) {
            log.error("Failed to send FCM push to token={}", token, ex);
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException ex) {
        MessagingErrorCode code = ex.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT;
    }
}
