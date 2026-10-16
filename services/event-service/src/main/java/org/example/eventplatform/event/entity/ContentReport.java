package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * Người dùng báo cáo nội dung xấu (tin nhắn, đánh giá, show trên Khám phá) theo quy định 1.2 của App Store;
 * Super Admin xem và xử lý (gỡ nội dung hoặc bỏ qua).
 */
@Entity
@Table(name = "content_reports", indexes = {
        @Index(name = "idx_report_status", columnList = "status, created_at"),
        @Index(name = "idx_report_target", columnList = "target_type, target_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    /** CHAT_MESSAGE, RATING hoặc SHOWCASE (id show). */
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** SPAM, OFFENSIVE, INAPPROPRIATE, SCAM, OTHER. */
    @Column(nullable = false, length = 20)
    private String reason;

    @Column(length = 500)
    private String note;

    /** Đoạn nội dung bị báo cáo chụp lại lúc báo cáo, để Super Admin xem được cả khi nội dung đã bị sửa hay xoá. */
    @Column(length = 500)
    private String snapshot;

    /** OPEN, REMOVED (đã gỡ nội dung), DISMISSED (bỏ qua). */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "handled_at")
    private LocalDateTime handledAt;
}
