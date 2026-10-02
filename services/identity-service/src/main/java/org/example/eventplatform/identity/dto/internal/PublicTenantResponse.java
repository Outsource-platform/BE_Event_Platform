package org.example.eventplatform.identity.dto.internal;

import lombok.Builder;

/**
 * Thông tin đoàn hiển thị cho khách trên sàn. Chỉ gồm các trường công khai —
 * tuyệt đối không kèm email, token xác thực hay danh sách thành viên.
 */
@Builder
public record PublicTenantResponse(
        Long id,
        String name,
        String domain,
        String logo,
        String category,
        String province,
        String ward,
        String primaryColorHex,
        String accentColorHex
) {
}
