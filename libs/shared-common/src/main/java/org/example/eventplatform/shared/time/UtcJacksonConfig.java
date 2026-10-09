package org.example.eventplatform.shared.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mọi {@link LocalDateTime} trả ra qua API đều là UTC do server sinh, nên được ghi có hậu tố {@code Z}
 * ({@code 2026-10-09T03:15:00Z}). Ngày giờ do người dùng nhập dùng {@code LocalDate} và {@code LocalTime} riêng,
 * không bị ảnh hưởng. Spring Boot tự nạp bean {@link JacksonModule} này vào mọi ObjectMapper của service.
 */
@Configuration
public class UtcJacksonConfig {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    public JacksonModule utcLocalDateTimeModule() {
        SimpleModule module = new SimpleModule("utc-local-date-time");
        module.addSerializer(LocalDateTime.class, new ValueSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
                gen.writeString(value.format(FORMAT) + "Z");
            }
        });
        return module;
    }
}
