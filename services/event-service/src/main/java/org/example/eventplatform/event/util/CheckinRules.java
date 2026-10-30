package org.example.eventplatform.event.util;

import org.example.eventplatform.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Quy tắc chung khi thành viên điểm danh tập kết, check-in và check-out. */
public final class CheckinRules {

    public static final int RADIUS_METERS = 500;
    public static final int CONCENTRATE_OPENS_MINUTES_BEFORE = 60;

    private static final DateTimeFormatter OPENS_AT = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private CheckinRules() {
    }

    /**
     * Bản app cũ và show chưa ghim vị trí không có toạ độ để so, nên bỏ qua thay vì chặn:
     * hai bản app chạy song song trên cùng backend, bản mới tự chặn khi không lấy được GPS.
     */
    public static void assertNear(Double lat, Double lng, Double pointLat, Double pointLng, String code, String message) {
        if (lat == null || lng == null || pointLat == null || pointLng == null) return;
        if (GeoUtils.distanceMeters(lat, lng, pointLat, pointLng) > RADIUS_METERS) {
            throw new ApiException(HttpStatus.CONFLICT, code, message);
        }
    }

    /** {@code now} là giờ Việt Nam vì ngày diễn và giờ tập trung là giờ tường do trưởng đoàn nhập. */
    public static void assertConcentrateOpen(LocalDate eventDate, LocalTime concentrateTime, LocalDateTime now) {
        if (eventDate == null || concentrateTime == null) return;
        LocalDateTime opensAt = eventDate.atTime(concentrateTime).minusMinutes(CONCENTRATE_OPENS_MINUTES_BEFORE);
        if (now.isBefore(opensAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "CONCENTRATE_NOT_OPEN",
                    "Chưa tới giờ điểm danh tập kết. Mở lúc " + opensAt.format(OPENS_AT) + ".");
        }
    }
}
