package org.example.eventplatform.event.dto;

import java.math.BigDecimal;

/**
 * Ví điểm của thành viên: [castFeeTotal] cát-xê gộp từ show đã hoàn thành, [fundTotal] phần trừ quỹ đoàn,
 * [commissionTotal] hoa hồng từ show họ tạo, [withdrawnTotal] đã rút hoặc đang chờ rút.
 * [available] = castFeeTotal - fundTotal + commissionTotal - withdrawnTotal.
 */
public record WalletSummary(BigDecimal available, BigDecimal castFeeTotal, BigDecimal fundTotal,
                            BigDecimal commissionTotal, BigDecimal withdrawnTotal) {
}
