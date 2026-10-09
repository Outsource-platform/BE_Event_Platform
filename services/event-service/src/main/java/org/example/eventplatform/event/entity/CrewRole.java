package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

/**
 * A tenant's own crew position catalog (e.g. department "Múa Lân" → role
 * "Đầu Lân"), used to structure show assignments instead of free-text.
 */
@Entity
@Table(name = "crew_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    private String department;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Mức trần cát-xê của vị trí. Gán người vào vị trí này là tự có mức này; để trống thì trưởng đoàn nhập tay như trước.
    @Column(name = "cast_fee", precision = 15, scale = 2)
    private java.math.BigDecimal castFee;
}
