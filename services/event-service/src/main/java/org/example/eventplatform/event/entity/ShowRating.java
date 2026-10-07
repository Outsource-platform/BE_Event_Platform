package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

/** Đánh giá của một khách cho show đã đăng trên bảng tin. Mỗi khách một đánh giá cho mỗi show, gửi lại thì cập nhật. */
@Entity
@Table(name = "show_ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_show_rating_user", columnNames = {"event_id", "user_id"}),
        indexes = @Index(name = "idx_show_rating_event", columnList = "event_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowRating extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Tên gốc của người đánh giá; chỉ phần đã che ("Ngu***") mới ra ngoài.
    private String userName;

    @Column(nullable = false)
    private int stars;

    @Column(length = 500)
    private String comment;
}
