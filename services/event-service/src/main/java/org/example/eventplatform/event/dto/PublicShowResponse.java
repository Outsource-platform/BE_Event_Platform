package org.example.eventplatform.event.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Show hiển thị công khai trên mục Khám phá. Cố ý không có địa chỉ, khách hàng, số tiền hay mã show:
 * khách vãng lai chỉ cần thấy đoàn nào đang hoạt động và diễn những dịp gì.
 */
@Builder
public record PublicShowResponse(
        Long id,
        String name,
        String type,
        String status,
        LocalDate eventDate,
        LocalTime startTime,
        String packageName,
        Long troupeId,
        String troupeName,
        String troupeLogo,
        String province
) {
}
