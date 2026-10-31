package org.example.eventplatform.event.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Show thành viên tự nhận. Không có tiền và thông tin khách: chưa nhận show thì chưa cần biết. */
public record OpenShowResponse(
        Long id,
        String name,
        String type,
        LocalDate eventDate,
        LocalTime startTime,
        String location,
        LocalTime concentrateTime,
        String concentrateLocation,
        Integer memberLimit,
        int acceptedCount
) {
}
