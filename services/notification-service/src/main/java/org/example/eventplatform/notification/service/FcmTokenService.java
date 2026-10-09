package org.example.eventplatform.notification.service;

import org.example.eventplatform.shared.time.Clocks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.notification.entity.FcmToken;
import org.example.eventplatform.notification.repository.FcmTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Value("${fcm.token-retention-days:90}")
    private int tokenRetentionDays;

    @Transactional
    public void registerToken(Long userId, String token) {
        LocalDateTime now = Clocks.utcNow();
        FcmToken existing = fcmTokenRepository.findByToken(token).orElse(null);
        if (existing != null) {
            existing.setUserId(userId);
            existing.setLastSeenAt(now);
            fcmTokenRepository.save(existing);
            return;
        }
        fcmTokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token(token)
                .lastSeenAt(now)
                .build());
    }

    @Transactional
    public void unregisterToken(Long userId, String token) {
        fcmTokenRepository.deleteByUserIdAndToken(userId, token);
    }

    @Transactional(readOnly = true)
    public List<FcmToken> getTokens(Long userId) {
        return fcmTokenRepository.findByUserId(userId);
    }

    @Transactional
    public void touchToken(String token) {
        fcmTokenRepository.findByToken(token).ifPresent(stored -> {
            stored.setLastSeenAt(Clocks.utcNow());
            fcmTokenRepository.save(stored);
        });
    }

    @Transactional
    public void removeInvalidToken(String token) {
        int removed = fcmTokenRepository.deleteByToken(token);
        if (removed > 0) {
            String suffix = token.length() > 8 ? token.substring(token.length() - 8) : token;
            log.info("Removed invalid FCM token ending ...{}", suffix);
        }
    }

    /** Nightly purge of tokens not seen for {@code fcm.token-retention-days}. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeStaleTokens() {
        LocalDateTime cutoff = Clocks.utcNow().minusDays(tokenRetentionDays);
        int deleted = fcmTokenRepository.deleteByLastSeenAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} stale FCM tokens older than {} days", deleted, tokenRetentionDays);
        }
    }
}
