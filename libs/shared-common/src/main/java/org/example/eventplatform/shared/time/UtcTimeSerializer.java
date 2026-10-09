package org.example.eventplatform.shared.time;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Trường giờ-trong-ngày do server sinh (UTC) được ghi dạng {@code 03:15:00Z} để client biết phải đổi sang giờ địa phương.
 * Chỉ gắn lên các trường như điểm danh; giờ do người dùng nhập (giờ bắt đầu...) vẫn ghi {@code 09:00:00} không có {@code Z}.
 */
public class UtcTimeSerializer extends ValueSerializer<LocalTime> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void serialize(LocalTime value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeString(value.format(FORMAT) + "Z");
    }
}
