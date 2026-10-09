package org.example.eventplatform.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.*;
import org.example.eventplatform.event.entity.AssignStatus;
import org.example.eventplatform.event.service.EventService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant/events")
@RequiredArgsConstructor
public class TenantEventController {

    private final EventService eventService;

    // ===== Admin của tenant =====

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EventResponse>> getAllMyEvents(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 20, sort = "eventDate") Pageable pageable) {
        return ResponseEntity.ok(eventService.getTenantEvents(principal.tenantId(), pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EventResponse>> getMySchedule(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam int month,
            @RequestParam int year,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(eventService.getTenantSchedule(principal.tenantId(), month, year, pageable));
    }

    @GetMapping("/by-customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventResponse>> getEventsForCustomer(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long customerId) {
        return ResponseEntity.ok(eventService.listTenantEventsForCustomer(principal.tenantId(), customerId));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MonthlySummaryResponse> getMySummary(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(eventService.getTenantMonthlySummary(principal.tenantId(), month, year));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> getMyEventDetail(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        EventResponse event = eventService.getEventDetail(id);
        if (event.getTenantId() == null || !event.getTenantId().equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(event);
    }

    @GetMapping("/{id}/with-members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventWithMembersResponse> getEventWithMembers(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        EventWithMembersResponse response = eventService.getEventDetailWithMembers(id);
        if (response.getEventInfo().getTenantId() == null || !response.getEventInfo().getTenantId().equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> acceptEvent(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long eventId) {
        eventService.acceptEvent(eventId, principal.tenantId());
        return ResponseEntity.ok("Đã nhận show thành công! Hãy gán anh em đi diễn.");
    }

    @PostMapping("/{eventId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectEvent(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long eventId) {
        eventService.rejectEvent(eventId, principal.tenantId());
        return ResponseEntity.ok("Đã từ chối show.");
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignMembers(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @RequestBody List<AssignMemberRequest> requests) {
        eventService.assignMembers(id, principal.tenantId(), requests);
        return ResponseEntity.ok("Đã gán thành viên!");
    }

    @PatchMapping("/{id}/concentrate-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateConcentrate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @RequestBody UpdateConcentrateRequest request) {
        return ResponseEntity.ok(eventService.updateConcentrateInfo(id, principal.tenantId(), request));
    }

    /** Đổi người thầu (hưởng hoa hồng) của show; contract_user_id trống là bỏ người thầu. */
    @PatchMapping("/{id}/contract")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateContract(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        return ResponseEntity.ok(eventService.updateContract(id, principal.tenantId(), body.get("contract_user_id")));
    }

    // ===== Member (anh em đi diễn) =====

    @GetMapping("/my-assignments")
    public ResponseEntity<List<AssignmentResponse>> getMyAssignments(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(eventService.getMyAssignedEvents(principal.userId()));
    }

    @PatchMapping("/assignments/{userEventId}/respond")
    public ResponseEntity<String> respondToAssignment(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long userEventId,
            @RequestParam AssignStatus status,
            @RequestParam(required = false) String note) {
        eventService.respondToAssignment(userEventId, principal, status, note);
        return ResponseEntity.ok("Đã phản hồi trạng thái show.");
    }

    @PostMapping("/assignments/{userEventId}/concentrate-check-in")
    public ResponseEntity<String> concentrateCheckIn(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long userEventId) {
        return ResponseEntity.ok(eventService.concentrateCheckIn(userEventId, principal));
    }

    @PostMapping("/assignments/{userEventId}/check-in")
    public ResponseEntity<String> checkIn(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long userEventId,
            @RequestParam String location,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        eventService.checkIn(userEventId, principal, location, lat, lng);
        return ResponseEntity.ok("Check-in thành công!");
    }

    @PostMapping("/assignments/{userEventId}/check-out")
    public ResponseEntity<String> checkOut(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long userEventId) {
        return ResponseEntity.ok(eventService.checkOut(userEventId, principal));
    }

    @PatchMapping("/assignments/{userEventId}/payroll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResponse> setPayroll(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long userEventId,
            @Valid @RequestBody List<PayrollItemRequest> items) {
        return ResponseEntity.ok(eventService.setPayrollItems(userEventId, principal.tenantId(), items));
    }

    @GetMapping("/dashboard-member/{userId}")
    public ResponseEntity<DashboardStatResponse> getDashboardStats(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long userId) {
        boolean isAdmin = principal.authorities().contains("ROLE_ADMIN") || principal.authorities().contains("ROLE_SUPER_ADMIN");
        if (!isAdmin && !userId.equals(principal.userId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(eventService.getMemberDashboardStats(principal.tenantId(), userId));
    }
}
