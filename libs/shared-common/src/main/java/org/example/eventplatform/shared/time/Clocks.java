package org.example.eventplatform.shared.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Quy ước thời gian của hệ thống: mọi mốc do server sinh ra (tạo, sửa, điểm danh, xử lý...) <b>lưu theo UTC</b> và trả ra
 * có hậu tố {@code Z}; app và web tự đổi sang giờ Việt Nam khi hiển thị. Luôn gọi các hàm ở đây thay vì
 * {@code LocalDateTime.now()} để kết quả không phụ thuộc múi giờ của máy chạy.
 *
 * <p>Ngày giờ do người dùng nhập (ngày diễn, giờ bắt đầu, giờ tập trung) là giờ tường ở Việt Nam, không có {@code Z}
 * và không bao giờ bị đổi múi giờ. Muốn so sánh chúng với "bây giờ" phải dùng {@link #vnNowTime()} hoặc {@link #vnToday()}.
 */
public final class Clocks {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private Clocks() {
    }

    /** Mốc thời gian để lưu xuống CSDL và trả ra API (UTC). */
    public static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Giờ trong ngày theo UTC, để lưu các trường chỉ có giờ do server sinh (điểm danh, check-out). */
    public static LocalTime utcNowTime() {
        return LocalTime.now(ZoneOffset.UTC);
    }

    /** Giờ hiện tại ở Việt Nam, để so với giờ người dùng nhập và để viết vào câu chữ thông báo. */
    public static LocalTime vnNowTime() {
        return LocalTime.now(VN);
    }

    /** Ngày hôm nay ở Việt Nam. */
    public static LocalDate vnToday() {
        return LocalDate.now(VN);
    }

    /** Đổi giờ-trong-ngày UTC sang giờ Việt Nam (để viết vào câu chữ). */
    public static LocalTime utcToVn(LocalTime utc) {
        return utc == null ? null : utc.plusHours(7);
    }
}
