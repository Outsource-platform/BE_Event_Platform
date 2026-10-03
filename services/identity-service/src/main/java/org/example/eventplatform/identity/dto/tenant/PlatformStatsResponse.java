package org.example.eventplatform.identity.dto.tenant;

import lombok.Builder;

/** Số liệu tổng quan phía identity cho trang thống kê của SUPER_ADMIN. */
@Builder
public record PlatformStatsResponse(long tenantTotal, long tenantActive, long customerTotal) {
}
