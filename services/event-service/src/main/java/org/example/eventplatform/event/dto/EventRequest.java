package org.example.eventplatform.event.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.eventplatform.event.entity.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Tên sự kiện không được để trống")
    private String name;

    private EventType type;

    @NotNull(message = "Ngày diễn ra không được để trống")
    private LocalDate eventDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @NotBlank(message = "Địa điểm không được để trống")
    private String location;

    @NotNull(message = "customerId không được để trống")
    private Long customerId;

    // Sàn chỉ định show này cho tenant nào; để trống nếu tenant tự tạo cho chính mình
    private Long tenantId;

    @DecimalMin(value = "0.0")
    private BigDecimal totalAmount;

    private String description;

    private LocalTime concentrateTime;
    private String concentrateLocation;
    private Double concentrateLat;
    private Double concentrateLng;

    // Gói biểu diễn tự chọn từ danh mục của tenant — để trống nếu không dùng gói có sẵn.
    private Long packageId;

    @DecimalMin(value = "0.0")
    private BigDecimal depositAmount;

    private String vehicleInfo;

    // Toạ độ điểm diễn thực tế — dùng để validate bán kính check-in của thành viên.
    private Double venueLat;
    private Double venueLng;
    private Integer checkinRadiusMeters;

    // % giữ lại làm quỹ đội chung trước khi chia cát-xê từng thành viên — để trống nếu không trích quỹ.
    @DecimalMin(value = "0.0")
    private BigDecimal teamFundPercent;
}
