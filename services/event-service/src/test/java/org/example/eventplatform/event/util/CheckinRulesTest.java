package org.example.eventplatform.event.util;

import org.example.eventplatform.shared.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckinRulesTest {

    private static final double LAT = 21.0285;
    private static final double LNG = 105.8542;
    // Một phần nghìn độ vĩ xấp xỉ 111m.
    private static final double ABOUT_400M = 0.0036;
    private static final double ABOUT_600M = 0.0054;

    private static final LocalDate SHOW_DAY = LocalDate.of(2026, 10, 31);
    private static final LocalTime GATHER = LocalTime.of(6, 30);

    @Test
    void trongBanKinh500mThiChoQua() {
        assertDoesNotThrow(() -> CheckinRules.assertNear(LAT + ABOUT_400M, LNG, LAT, LNG, "NOT_NEAR_VENUE", "xa"));
    }

    @Test
    void ngoaiBanKinh500mThiBaoDungMaLoi() {
        ApiException ex = assertThrows(ApiException.class,
                () -> CheckinRules.assertNear(LAT + ABOUT_600M, LNG, LAT, LNG, "NOT_NEAR_VENUE", "Bạn đang không ở gần điểm diễn"));
        assertEquals("NOT_NEAR_VENUE", ex.getCode());
        assertEquals("Bạn đang không ở gần điểm diễn", ex.getMessage());
    }

    @Test
    void thieuToaDoThiBoQua() {
        assertDoesNotThrow(() -> CheckinRules.assertNear(null, null, LAT, LNG, "NOT_NEAR_VENUE", "xa"));
        assertDoesNotThrow(() -> CheckinRules.assertNear(LAT, LNG, null, null, "NOT_NEAR_VENUE", "xa"));
    }

    @Test
    void truocGioTapKetHonMotTiengThiChuaMo() {
        ApiException ex = assertThrows(ApiException.class,
                () -> CheckinRules.assertConcentrateOpen(SHOW_DAY, GATHER, LocalDateTime.of(2026, 10, 31, 5, 29)));
        assertEquals("CONCENTRATE_NOT_OPEN", ex.getCode());
    }

    @Test
    void dungMotTiengTruocGioTapKetThiMo() {
        assertDoesNotThrow(() -> CheckinRules.assertConcentrateOpen(SHOW_DAY, GATHER, LocalDateTime.of(2026, 10, 31, 5, 30)));
    }

    @Test
    void ngayHomTruocVanChuaMo() {
        assertThrows(ApiException.class,
                () -> CheckinRules.assertConcentrateOpen(SHOW_DAY, GATHER, LocalDateTime.of(2026, 10, 30, 13, 36)));
    }

    @Test
    void khongCoGioTapKetThiKhongChan() {
        assertDoesNotThrow(() -> CheckinRules.assertConcentrateOpen(SHOW_DAY, null, LocalDateTime.of(2026, 10, 30, 13, 36)));
    }
}
