package org.example.eventplatform.identity.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.dto.internal.AdminContactResponse;
import org.example.eventplatform.identity.dto.internal.PublicTenantResponse;
import org.example.eventplatform.identity.dto.internal.TenantSummaryResponse;
import org.example.eventplatform.identity.dto.internal.UserContactResponse;
import org.example.eventplatform.identity.entity.Tenant;
import org.example.eventplatform.identity.entity.User;
import org.example.eventplatform.identity.repository.TenantRepository;
import org.example.eventplatform.identity.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * Service-to-service only. Protected by {@code X-Internal-Token}; never routed
 * through the public api-gateway ({@code /api/internal/**} is absent from RouteTable).
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    @GetMapping("/tenants/{tenantId}/admins")
    public ResponseEntity<List<AdminContactResponse>> getTenantAdmins(@PathVariable Long tenantId) {
        List<AdminContactResponse> admins = userRepository.findByTenantIdAndRoleName(tenantId, "ADMIN").stream()
                .map(this::toAdminContact)
                .toList();
        return ResponseEntity.ok(admins);
    }

    /** Danh sách đoàn cho sàn khách hàng — chỉ đoàn đang hoạt động, chỉ trường công khai. */
    @GetMapping("/tenants/public")
    public ResponseEntity<List<PublicTenantResponse>> getPublicTenants() {
        List<PublicTenantResponse> tenants = tenantRepository.findAll().stream()
                .filter(Tenant::isActive)
                .map(this::toPublicTenant)
                .toList();
        return ResponseEntity.ok(tenants);
    }

    private PublicTenantResponse toPublicTenant(Tenant tenant) {
        return PublicTenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .domain(tenant.getDomain())
                .logo(tenant.getLogo())
                .category(tenant.getCategory())
                .province(tenant.getProvince())
                .ward(tenant.getWard())
                .primaryColorHex(tenant.getPrimaryColorHex())
                .accentColorHex(tenant.getAccentColorHex())
                .build();
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantSummaryResponse> getTenant(@PathVariable Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tenant với ID: " + tenantId));
        return ResponseEntity.ok(toTenantSummary(tenant));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserContactResponse> getUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user với ID: " + userId));
        return ResponseEntity.ok(toUserContact(user));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserContactResponse>> getUsers(@RequestParam("ids") Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<UserContactResponse> users = userRepository.findAllById(ids).stream()
                .map(this::toUserContact)
                .toList();
        return ResponseEntity.ok(users);
    }

    private AdminContactResponse toAdminContact(User user) {
        return AdminContactResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    private UserContactResponse toUserContact(User user) {
        return UserContactResponse.builder()
                .userId(user.getId())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roleName(user.getRoles() != null ? user.getRoles().getName() : null)
                .email(user.getEmail())
                .availabilityStatus(user.getAvailabilityStatus())
                .commissionRate(user.getCommissionRate())
                .teamFundPercent(user.getTeamFundPercent())
                .joinedDate(user.getJoinedDate())
                .seniority(user.getSeniority())
                .bankName(user.getBankName())
                .bankAccountNumber(user.getBankAccountNumber())
                .bankAccountHolder(user.getBankAccountHolder())
                .build();
    }

    private TenantSummaryResponse toTenantSummary(Tenant tenant) {
        return TenantSummaryResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .domain(tenant.getDomain())
                .email(tenant.getEmail())
                .active(tenant.isActive())
                .build();
    }
}
