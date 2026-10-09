package org.example.eventplatform.identity.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.eventplatform.identity.entity.AvailabilityStatus;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContactResponse {
    private Long userId;
    private Long tenantId;
    private String username;
    private String fullName;
    private String roleName;
    private String email;
    private AvailabilityStatus availabilityStatus;
    private BigDecimal commissionRate;
    private BigDecimal teamFundPercent;
    private java.time.LocalDate joinedDate;
    private Integer seniority;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
}
