package org.example.eventplatform.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.eventplatform.event.entity.EventStatus;
import org.example.eventplatform.event.entity.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String showCode;
    private String name;
    private EventType type;
    private String typeDisplayName;
    private EventStatus status;
    private String statusDisplayName;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private Long customerId;
    private String customerName;
    private Long tenantId;
    private String tenantName;
    private String vendorBusinessName;
    private String serviceCategoryName;
    private LocalTime concentrateTime;
    private String concentrateLocation;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private Long packageId;
    private String packageName;
    private BigDecimal depositAmount;
    private BigDecimal depositPercent;
    private String vehicleInfo;
    private String description;
    private Double venueLat;
    private Double venueLng;
    private Integer checkinRadiusMeters;
    private BigDecimal teamFundPercent;
    private BigDecimal teamFundAmount;
    private Long createdByUserId;
    private BigDecimal creatorCommissionAmount;
    private LocalDateTime createdAt;
}
