package org.example.eventplatform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * Bài viết công khai (trang Tin tức, phục vụ SEO). {@code tenantId} null là bài của sàn do Super Admin viết;
 * có giá trị là bài của một đơn vị. Đăng bài miễn phí, không giới hạn số bài hay thời hạn hiển thị.
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_status_published", columnList = "status, published_at"),
        @Index(name = "idx_posts_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    private Long authorUserId;

    // Ảnh chụp tại lúc đăng, để trang công khai hiện tác giả mà không phải gọi identity-service mỗi lần xem.
    private String authorName;

    private String authorDomain;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private String coverImage;

    // Cấu hình SEO riêng; để trống thì trang dùng title/excerpt.
    private String seoTitle;

    @Column(length = 500)
    private String seoDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    private LocalDateTime publishedAt;

    // Lần "đẩy tin" gần nhất: danh sách công khai xếp theo mốc này (nếu có) thay vì ngày đăng.
    private LocalDateTime pushedAt;
}
