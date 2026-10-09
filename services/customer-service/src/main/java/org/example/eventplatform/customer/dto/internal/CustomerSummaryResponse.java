package org.example.eventplatform.customer.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryResponse {
    private Long id;
    private Long tenantId;
    private String fullName;
    private String phone;
    private String email;
    private Long userId;
    private Long assignedToUserId;
}
