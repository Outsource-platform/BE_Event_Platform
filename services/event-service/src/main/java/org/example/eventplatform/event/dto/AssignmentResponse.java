package org.example.eventplatform.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.eventplatform.event.entity.AssignStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.example.eventplatform.shared.time.UtcTimeSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private Long id;
    private Long eventId;
    private String eventName;
    private LocalDate eventDate;
    private String location;

    private Long userId;
    private String userFullName;
    private String position;
    private Long crewRoleId;
    private String crewRoleDepartment;
    private String crewRoleName;
    private AssignStatus status;
    private String statusDisplayName;
    private String note;

    @JsonSerialize(using = UtcTimeSerializer.class)
    private LocalTime actualConcentrateAt;
    @JsonSerialize(using = UtcTimeSerializer.class)
    private LocalTime checkinAt;
    @JsonSerialize(using = UtcTimeSerializer.class)
    private LocalTime checkoutAt;
    private Double checkinLat;
    private Double checkinLng;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalTime concentrateTime;
    private String concentrateLocation;

    private List<PayrollItemResponse> payrollItems;
    private BigDecimal totalPayroll;

    private List<Teammate> teammates;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Teammate {
        private Long userId;
        private String fullName;
        private String position;
        private Long crewRoleId;
        private String crewRoleDepartment;
        private String crewRoleName;
        private AssignStatus status;
        private String statusDisplayName;
    }
}
