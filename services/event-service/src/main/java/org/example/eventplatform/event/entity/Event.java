package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.eventplatform.shared.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_tenant_show_code", columnNames = {"tenant_id", "show_code"}),
        indexes = {
                // Lịch theo tháng và danh sách show của đơn vị lọc theo tenant + ngày
                @Index(name = "idx_events_tenant_date", columnList = "tenant_id, event_date"),
                // "Show của khách" tra theo customer_id
                @Index(name = "idx_events_customer", columnList = "customer_id"),
                // Thống kê toàn sàn đếm theo trạng thái
                @Index(name = "idx_events_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Mã đọc được trong đoàn, ví dụ abc-2610-0007. Cấp một lần lúc tạo show. */
    @Column(name = "show_code", length = 40)
    private String showCode;

    @Enumerated(EnumType.STRING)
    private EventType type;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    private String location;

    // customer-service and identity-service own these rows — database-per-service,
    // so only the foreign id crosses the boundary, never a JPA relation.
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "tenant_id")
    private Long tenantId;

    private BigDecimal totalAmount;
    private BigDecimal platformFee;

    private LocalTime concentrateTime;
    private String concentrateLocation;

    @Column(name = "concentrate_lat")
    private Double concentrateLat;

    @Column(name = "concentrate_lng")
    private Double concentrateLng;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    // ShowPackage lives in this same service/DB, but is still looked up by id
    // (not a JPA relation) — a package is a catalog pick, not an ownership link,
    // and its name is denormalized here so it survives the package being edited/deleted later.
    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;

    @Column(name = "vehicle_info")
    private String vehicleInfo;

    @Column(name = "venue_lat")
    private Double venueLat;

    @Column(name = "venue_lng")
    private Double venueLng;

    @Column(name = "checkin_radius_meters")
    private Integer checkinRadiusMeters;

    // % of totalAmount the tenant keeps as a shared team fund before splitting
    // the rest into per-member payroll_items — null/0 means no fund is kept.
    @Column(name = "team_fund_percent")
    private BigDecimal teamFundPercent;

    // Set only when a TN_MEMBER self-creates the show (not the tenant admin,
    // not a platform push) — distinct from the display-only `createdBy`
    // username string, needed as a real FK to look up/pay the commission.
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "creator_commission_amount")
    private BigDecimal creatorCommissionAmount;

    // Trưng bày công khai: đoàn tự chọn đăng show đã diễn lên bảng tin kèm ảnh, video và mô tả.
    // Mặc định không hiện, để tên khách và chi tiết riêng tư của show không tự lộ ra ngoài.
    @Column(name = "showcase_published")
    private Boolean showcasePublished;

    // Người đăng show lên Khám phá: khách bấm Chat ở bài này thì nhắn thẳng tới người đó.
    @Column(name = "showcase_published_by")
    private Long showcasePublishedBy;

    @Column(name = "showcase_title")
    private String showcaseTitle;

    @Column(name = "showcase_description", columnDefinition = "TEXT")
    private String showcaseDescription;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserEvent> assignedMembers = new ArrayList<>();
}
