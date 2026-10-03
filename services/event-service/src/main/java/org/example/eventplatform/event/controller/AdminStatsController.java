package org.example.eventplatform.event.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.AdminStatsResponse;
import org.example.eventplatform.event.entity.EventStatus;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowPackageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Thống kê toàn sàn, chỉ SUPER_ADMIN — không lọc theo tenant. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminStatsController {

    private final EventRepository eventRepository;
    private final ShowPackageRepository showPackageRepository;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (EventStatus status : EventStatus.values()) {
            byStatus.put(status.name(), eventRepository.countByStatus(status));
        }
        return ResponseEntity.ok(AdminStatsResponse.builder()
                .eventTotal(eventRepository.count())
                .eventsByStatus(byStatus)
                .activePackages(showPackageRepository.findByActiveTrue().size())
                .build());
    }
}
