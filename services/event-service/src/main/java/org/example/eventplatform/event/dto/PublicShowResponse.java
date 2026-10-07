package org.example.eventplatform.event.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Show trưng bày công khai trên bảng tin Khám phá. Cố ý không có địa chỉ, khách hàng, số tiền hay mã show:
 * khách vãng lai chỉ cần thấy đoàn đã diễn gì, trông thế nào và dùng gói nào.
 */
@Builder
public record PublicShowResponse(
        Long id,
        String title,
        String description,
        String type,
        String status,
        LocalDate eventDate,
        LocalTime startTime,
        List<MediaDto> media,
        PublicPackageResponse showPackage,
        Long troupeId,
        String troupeName,
        String troupeLogo,
        String province
) {
}
