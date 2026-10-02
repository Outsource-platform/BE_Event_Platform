package org.example.eventplatform.event.dto;

import lombok.Builder;

import java.util.List;

/**
 * Payload gộp cho trang chủ app của khách — phỏng theo {@code HomeAppResponse} của
 * rencity-platform-spring, rút gọn còn đúng phần sàn này cần. Gộp vào một lần gọi
 * để trang chủ không phải chờ nhiều request nối tiếp.
 */
@Builder
public record HomeAppResponse(
        List<BannerBrief> banners,
        List<CategoryBrief> categories,
        List<ProvinceBrief> discovers,
        List<PublicTroupeResponse> featuredTroupes,
        List<PublicPackageResponse> featuredPackages
) {

    /** Một banner trong slider đầu trang chủ. */
    @Builder
    public record BannerBrief(Long id, String title, String subtitle, String imageUrl, String linkUrl) {
    }

    /** Loại hình dịch vụ + số đơn vị đang hoạt động thuộc loại đó. */
    @Builder
    public record CategoryBrief(Long id, String code, String name, String description, int troupeCount) {
    }

    /** Một khu vực trong phần "khám phá khu vực". */
    @Builder
    public record ProvinceBrief(String province, int troupeCount) {
    }
}
