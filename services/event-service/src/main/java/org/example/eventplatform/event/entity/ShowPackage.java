package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * A tenant's own performance package catalog (e.g. "Gói Tứ Quý Hưng Long") —
 * picked when creating an Event, not shared across tenants.
 */
@Entity
@Table(name = "show_packages", indexes = {
        @Index(name = "idx_show_packages_tenant", columnList = "tenant_id"),
        @Index(name = "idx_show_packages_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowPackage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;

    @Builder.Default
    private boolean active = true;
}
