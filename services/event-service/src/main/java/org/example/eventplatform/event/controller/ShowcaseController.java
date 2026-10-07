package org.example.eventplatform.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.ShowcaseRequest;
import org.example.eventplatform.event.dto.ShowcaseResponse;
import org.example.eventplatform.event.service.ShowcaseService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/events/{eventId}/showcase")
@RequiredArgsConstructor
public class ShowcaseController {

    private final ShowcaseService showcaseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowcaseResponse> get(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long eventId) {
        return ResponseEntity.ok(showcaseService.get(eventId, principal.tenantId()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowcaseResponse> save(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long eventId,
                                                 @Valid @RequestBody ShowcaseRequest request) {
        return ResponseEntity.ok(showcaseService.save(eventId, principal.tenantId(), request));
    }
}
