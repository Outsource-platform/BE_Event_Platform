package org.example.eventplatform.identity.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Long tenantId;
    private String tenantName;
    private String roleName;
    private Set<String> authorities;
    private BigDecimal commissionRate;
    private BigDecimal teamFundPercent;
    private java.time.LocalDate joinedDate;
    private Integer seniority;
    private String category;
    private String province;
    private String ward;
    private String primaryColorHex;
    private String accentColorHex;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
}
