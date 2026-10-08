package org.example.eventplatform.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.dto.auth.AuthTokenResponse;
import org.example.eventplatform.identity.dto.auth.CustomerRegisterRequest;
import org.example.eventplatform.identity.dto.auth.DeleteAccountRequest;
import org.example.eventplatform.identity.dto.auth.LoginRequest;
import org.example.eventplatform.identity.dto.auth.RefreshTokenRequest;
import org.example.eventplatform.identity.dto.auth.TenantLookupResponse;
import org.example.eventplatform.identity.dto.auth.UpdateBankAccountRequest;
import org.example.eventplatform.identity.dto.auth.UserSummaryResponse;
import org.example.eventplatform.identity.service.AuthService;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/customer/register")
    public ResponseEntity<AuthTokenResponse> registerCustomer(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.ok(authService.registerCustomer(request));
    }

    @GetMapping("/tenant-lookup")
    public ResponseEntity<TenantLookupResponse> lookupTenant(@RequestParam String domain) {
        return ResponseEntity.ok(authService.lookupTenant(domain));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /** Người dùng tự xoá tài khoản của mình (xác nhận bằng mật khẩu). */
    @PostMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal JwtPrincipal principal,
                                              @Valid @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(principal.userId(), request.getPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> getMe(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(authService.getMe(principal.userId()));
    }

    /** Thành viên tự khai tài khoản nhận tiền trên hồ sơ của mình. */
    @PatchMapping("/me/bank-account")
    public ResponseEntity<UserSummaryResponse> updateBankAccount(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody UpdateBankAccountRequest request) {
        return ResponseEntity.ok(authService.updateBankAccount(
                principal.userId(),
                request.getBankName(),
                request.getBankAccountNumber(),
                request.getBankAccountHolder()));
    }
}
