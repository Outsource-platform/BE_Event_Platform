package org.example.eventplatform.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private BigDecimal amount;
    private String status;
    private String statusDisplayName;
    private String note;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
