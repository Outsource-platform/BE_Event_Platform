package org.example.eventplatform.identity.time;

import org.example.eventplatform.shared.time.Clocks;
import org.example.eventplatform.shared.time.UtcJacksonConfig;
import org.example.eventplatform.shared.time.UtcTimeSerializer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtcJsonTest {

    record Sample(
            LocalDateTime createdAt,
            @JsonSerialize(using = UtcTimeSerializer.class) LocalTime checkinAt,
            LocalTime startTime) {
    }

    private final JsonMapper mapper = JsonMapper.builder().addModule(new UtcJacksonConfig().utcLocalDateTimeModule()).build();

    @Test
    void mốcThờiGianDoServerSinhCóHậuTốZ() {
        String json = mapper.writeValueAsString(new Sample(LocalDateTime.of(2026, 10, 9, 3, 15, 7), LocalTime.of(15, 5, 9), LocalTime.of(9, 0)));
        assertTrue(json.contains("\"createdAt\":\"2026-10-09T03:15:07Z\""), json);
        assertTrue(json.contains("\"checkinAt\":\"15:05:09Z\""), json);
    }

    @Test
    void giờDoNgườiDùngNhậpKhôngBịGắnZ() {
        String json = mapper.writeValueAsString(new Sample(LocalDateTime.of(2026, 10, 9, 3, 15), LocalTime.of(1, 2), LocalTime.of(9, 0)));
        assertTrue(json.contains("\"startTime\":\"09:00:00\"") || json.contains("\"startTime\":[9,0]") || json.contains("\"startTime\":\"09:00\""), json);
        assertTrue(!json.contains("\"startTime\":\"09:00:00Z\""), json);
    }

    @Test
    void đồngHồUtcKhôngPhụThuộcMúiGiờMáy() {
        java.util.TimeZone old = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            long diff = java.time.Duration.between(Clocks.utcNow(), LocalDateTime.now(java.time.ZoneOffset.UTC)).abs().getSeconds();
            assertTrue(diff <= 1, "utcNow phải là UTC dù JVM đang giờ Việt Nam");
        } finally {
            java.util.TimeZone.setDefault(old);
        }
    }
}
