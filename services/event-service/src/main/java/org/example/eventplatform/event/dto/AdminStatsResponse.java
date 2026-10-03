package org.example.eventplatform.event.dto;

import lombok.Builder;

import java.util.Map;

/** Số liệu phía event-service cho trang thống kê của SUPER_ADMIN. */
@Builder
public record AdminStatsResponse(long eventTotal, Map<String, Long> eventsByStatus, long activePackages) {
}
