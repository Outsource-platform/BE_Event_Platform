package org.example.eventplatform.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

@Entity
@Table(name = "customers",
        indexes = {
                @Index(name = "idx_customer_full_name", columnList = "fullName"),
                @Index(name = "idx_customer_type", columnList = "type"),
                @Index(name = "idx_customer_user", columnList = "user_id")
        },
        // Unique per tenant, not globally — the old monolith's global unique(phone)
        // didn't match its own per-tenant duplicate check.
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_tenant_phone", columnNames = {"tenant_id", "phone"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    private String email;
    private String address;

    @Enumerated(EnumType.STRING)
    private CustomerType type;

    private String note;

    @Builder.Default
    private boolean active = true;

    // identity-service owns the User row — only the id crosses the boundary.
    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    /**
     * Links this per-tenant CRM row to the marketplace CUSTOMER account
     * (identity-service User with role CUSTOMER, tenantId = null).
     * Nullable because admin-created CRM customers may have no app account.
     */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
