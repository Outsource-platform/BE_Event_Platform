package org.example.eventplatform.event.dto;

import java.math.BigDecimal;

/** Một gói show của đoàn trên trang chi tiết show; [selected] là gói đoàn đã dùng cho show này (cột events.package_id). */
public record PackageOption(Long id, String name, String description, BigDecimal price, boolean selected) {
}
