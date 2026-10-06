package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A member's request to cash out their earned points balance — the app
 * itself never moves money, this is just a workflow record so the tenant
 * admin knows who to pay outside the app (bank transfer, cash, ...). Keeping
 * this off the in-app-payment path is deliberate: see the "Ví Điểm" wording
 * throughout the member UI, chosen to avoid Apple's IAP review flagging what
 * would otherwise look like an in-app real-money transaction.
 */
@Entity
@Table(name = "withdrawal_requests", indexes = {
        @Index(name = "idx_withdrawal_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_withdrawal_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
