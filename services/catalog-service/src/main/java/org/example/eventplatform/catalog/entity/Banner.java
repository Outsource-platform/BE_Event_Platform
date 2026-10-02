package org.example.eventplatform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

/**
 * Banner quảng cáo trượt ở đầu trang chủ app. Do SUPER_ADMIN của sàn quản lý,
 * không thuộc về đoàn nào. Khi chưa có ảnh thì app tự vẽ nền màu cùng tiêu đề.
 */
@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String subtitle;

    private String imageUrl;

    // Đường dẫn khi khách bấm vào banner: URL web hoặc route trong app. Có thể để trống.
    private String linkUrl;

    // Số nhỏ hiển thị trước
    @Builder.Default
    private int sortOrder = 0;

    @Builder.Default
    private boolean active = true;
}
