package org.example.eventplatform.event.dto;

import lombok.Builder;

import java.util.List;

/** Nội dung trưng bày hiện tại của một show, để đoàn mở lại màn chỉnh sửa. */
@Builder
public record ShowcaseResponse(
        Long eventId,
        boolean published,
        String title,
        String description,
        List<MediaDto> media
) {
}
