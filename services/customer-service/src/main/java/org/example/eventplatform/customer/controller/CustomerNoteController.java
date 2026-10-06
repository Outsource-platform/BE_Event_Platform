package org.example.eventplatform.customer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.customer.dto.CustomerNoteRequest;
import org.example.eventplatform.customer.dto.CustomerNoteResponse;
import org.example.eventplatform.customer.service.CustomerNoteService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CustomerNoteController {

    private final CustomerNoteService customerNoteService;

    @GetMapping
    public ResponseEntity<List<CustomerNoteResponse>> list(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long customerId) {
        return ResponseEntity.ok(customerNoteService.list(principal.tenantId(), customerId));
    }

    @PostMapping
    public ResponseEntity<CustomerNoteResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerNoteRequest request) {
        return new ResponseEntity<>(
                customerNoteService.create(principal.tenantId(), principal.userId(), principal.username(), customerId, request),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long customerId,
            @PathVariable Long noteId) {
        customerNoteService.delete(principal.tenantId(), customerId, noteId);
        return ResponseEntity.noContent().build();
    }
}
