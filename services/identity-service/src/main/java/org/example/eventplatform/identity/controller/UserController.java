package org.example.eventplatform.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.dto.auth.UpdateBankAccountRequest;
import org.example.eventplatform.identity.dto.user.CreateMemberRequest;
import org.example.eventplatform.identity.dto.user.MemberResponse;
import org.example.eventplatform.identity.dto.user.UpdateAvailabilityRequest;
import org.example.eventplatform.identity.dto.user.UpdateCommissionRateRequest;
import org.example.eventplatform.identity.service.UserService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant admin managing their own troupe's member roster — separate from
 * TenantController (tenant self-registration) and AuthController (login).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<MemberResponse>> listMembers(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return ResponseEntity.ok(userService.listTenantMembers(principal.tenantId(), pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> createMember(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateMemberRequest request) {
        return new ResponseEntity<>(userService.createTenantMember(principal.tenantId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> getMember(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.getTenantMember(principal.tenantId(), id));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> updateAvailability(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(userService.updateAvailability(principal.tenantId(), id, request.getAvailabilityStatus()));
    }

    @PatchMapping("/{id}/commission-rate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> updateCommissionRate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommissionRateRequest request) {
        return ResponseEntity.ok(userService.updateCommissionRate(principal.tenantId(), id, request.getCommissionRate()));
    }

    @PatchMapping("/{id}/bank-account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MemberResponse> updateBankAccount(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @RequestBody UpdateBankAccountRequest request) {
        return ResponseEntity.ok(userService.updateBankAccount(
                principal.tenantId(),
                id,
                request.getBankName(),
                request.getBankAccountNumber(),
                request.getBankAccountHolder()));
    }
}
