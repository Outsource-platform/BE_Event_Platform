package org.example.eventplatform.notification.repository;

import org.example.eventplatform.notification.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    boolean existsByMessageIdAndUserId(String messageId, Long userId);

    Page<UserNotification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNullAndDeletedFalse(Long userId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.readAt = :readAt, n.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE n.id = :id AND n.userId = :userId AND n.readAt IS NULL AND n.deleted = false")
    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE UserNotification n SET n.readAt = :readAt, n.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE n.userId = :userId AND n.readAt IS NULL AND n.deleted = false")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
