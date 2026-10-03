package org.example.eventplatform.catalog.dto;

import lombok.Builder;
import org.example.eventplatform.catalog.entity.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Đầy đủ một bài viết. {@code related} chỉ có ở trang chi tiết công khai. */
@Builder
public record PostResponse(
        Long id,
        Long tenantId,
        String authorName,
        String authorDomain,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImage,
        String seoTitle,
        String seoDescription,
        PostStatus status,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        List<PostSummary> related
) {
}
