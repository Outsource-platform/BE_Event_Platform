package org.example.eventplatform.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.dto.tenant.PlatformStatsResponse;
import org.example.eventplatform.identity.dto.tenant.TenantRegisterRequest;
import org.example.eventplatform.identity.dto.tenant.TenantRegisterResponse;
import org.example.eventplatform.identity.dto.tenant.TenantResponse;
import org.example.eventplatform.identity.dto.tenant.UpdateTenantActiveRequest;
import org.example.eventplatform.identity.dto.tenant.UpdateTenantThemeRequest;
import org.example.eventplatform.identity.service.TenantService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/register")
    public ResponseEntity<TenantRegisterResponse> register(@Valid @RequestBody TenantRegisterRequest request) {
        return new ResponseEntity<>(tenantService.registerTenant(request), HttpStatus.CREATED);
    }

    // RBAC demo for Phase 1's DoD: only the platform's SUPER_ADMIN can list every tenant.
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<TenantResponse>> getAll() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PlatformStatsResponse> stats() {
        return ResponseEntity.ok(tenantService.getPlatformStats());
    }

    /** Khoá/mở một đơn vị: đơn vị bị khoá không đăng nhập được và biến mất khỏi sàn. */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantResponse> setActive(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateTenantActiveRequest request) {
        return ResponseEntity.ok(tenantService.setActive(id, request.getActive()));
    }

    @PatchMapping("/me/theme")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> updateMyTheme(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateTenantThemeRequest request) {
        return ResponseEntity.ok(tenantService.updateMyTheme(principal.tenantId(), request));
    }
}
