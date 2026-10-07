package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

/** Ảnh hoặc video của một show trưng bày. Tệp nằm ở kho lưu trữ, bảng này chỉ giữ đường dẫn và thứ tự hiển thị. */
@Entity
@Table(name = "show_media", indexes = @Index(name = "idx_show_media_event", columnList = "event_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType type;

    @Column(nullable = false, length = 600)
    private String url;

    @Column(name = "sort_order")
    private int sortOrder;
}
