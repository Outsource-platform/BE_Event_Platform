package org.example.eventplatform.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.WithdrawalCreateRequest;
import org.example.eventplatform.event.dto.WithdrawalResponse;
import org.example.eventplatform.event.service.WithdrawalService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.example.eventplatform.event.dto.WalletSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @GetMapping("/my-balance")
    public ResponseEntity<WalletSummary> myBalance(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(withdrawalService.wallet(principal.tenantId(), principal.userId()));
    }

    @PostMapping
    public ResponseEntity<WithdrawalResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody WithdrawalCreateRequest request) {
        return new ResponseEntity<>(withdrawalService.create(principal.tenantId(), principal.userId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<List<WithdrawalResponse>> myRequests(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(withdrawalService.listForMember(principal.userId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WithdrawalResponse>> listForTenant(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(withdrawalService.listForTenant(principal.tenantId()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalResponse> approve(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(withdrawalService.approve(principal.tenantId(), id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawalResponse> reject(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(withdrawalService.reject(principal.tenantId(), id));
    }
}
