package org.example.eventplatform.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.ReportDtos;
import org.example.eventplatform.event.service.ContentReportService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Người dùng đã đăng nhập báo cáo nội dung; Super Admin xem và xử lý. */
@RestController
@RequiredArgsConstructor
public class ContentReportController {

    private final ContentReportService reportService;

    @PostMapping("/api/reports")
    public ResponseEntity<ReportDtos.Item> create(@AuthenticationPrincipal JwtPrincipal principal,
                                                  @Valid @RequestBody ReportDtos.CreateRequest request) {
        return ResponseEntity.ok(reportService.create(principal.userId(), request));
    }

    @GetMapping("/api/admin/reports")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ReportDtos.Page> list(@RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.list(status, page, size));
    }

    @PatchMapping("/api/admin/reports/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ReportDtos.Item> handle(@PathVariable Long id, @Valid @RequestBody ReportDtos.HandleRequest request) {
        return ResponseEntity.ok(reportService.handle(id, request.getStatus()));
    }
}
