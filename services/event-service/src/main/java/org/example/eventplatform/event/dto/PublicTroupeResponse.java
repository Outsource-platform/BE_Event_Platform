package org.example.eventplatform.event.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/** Thẻ đoàn hiển thị trên sàn cho khách. */
@Builder
public record PublicTroupeResponse(
        Long id,
        String name,
        String domain,
        String logo,
        String category,
        String province,
        String ward,
        String primaryColorHex,
        String accentColorHex,
        int packageCount,
        BigDecimal fromPrice,
        List<PublicPackageResponse> packages
) {
}
