package org.example.eventplatform.catalog.dto;

import lombok.Builder;
import org.example.eventplatform.catalog.entity.PostStatus;

import java.time.LocalDateTime;

/** Thẻ bài viết trong danh sách (không kèm nội dung). */
@Builder
public record PostSummary(
        Long id,
        Long tenantId,
        String authorName,
        String authorDomain,
        String title,
        String slug,
        String excerpt,
        String coverImage,
        PostStatus status,
        LocalDateTime publishedAt
) {
}
