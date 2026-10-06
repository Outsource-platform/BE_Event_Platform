package org.example.eventplatform.event.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_event", indexes = @Index(name = "idx_user_event_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    // identity-service owns the User row — only the id crosses the boundary.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String position;

    // CrewRole lives in this same service/DB — looked up by id, not a JPA
    // relation, so a role can be renamed/removed without touching past assignments.
    @Column(name = "crew_role_id")
    private Long crewRoleId;

    @Enumerated(EnumType.STRING)
    private AssignStatus status;

    private String note;
    private LocalDateTime respondedAt;

    private LocalTime checkinAt;
    private LocalTime checkoutAt;
    private String checkinLocation;

    @Column(name = "checkin_lat")
    private Double checkinLat;

    @Column(name = "checkin_lng")
    private Double checkinLng;

    private LocalTime actualConcentrateAt;

    // Total payout for this assignment — kept in sync with the sum of
    // payrollItems below so the existing dashboard earnings query (which sums
    // this single column) doesn't need to change.
    @Column(name = "salary", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal salary = BigDecimal.ZERO;

    @OneToMany(mappedBy = "userEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserEventPayrollItem> payrollItems = new ArrayList<>();
}
