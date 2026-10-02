package org.example.eventplatform.identity.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.dto.tenant.TenantRegisterRequest;
import org.example.eventplatform.identity.dto.tenant.TenantRegisterResponse;
import org.example.eventplatform.identity.dto.tenant.TenantResponse;
import org.example.eventplatform.identity.dto.tenant.UpdateTenantThemeRequest;
import org.example.eventplatform.identity.entity.RegistrationStatus;
import org.example.eventplatform.identity.entity.Role;
import org.example.eventplatform.identity.entity.Tenant;
import org.example.eventplatform.identity.entity.User;
import org.example.eventplatform.identity.entity.UserStatus;
import org.example.eventplatform.identity.repository.RoleRepository;
import org.example.eventplatform.identity.repository.TenantRepository;
import org.example.eventplatform.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TenantRegisterResponse registerTenant(TenantRegisterRequest request) {
        if (tenantRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trên hệ thống");
        }
        if (tenantRepository.existsByDomain(request.getDomain())) {
            throw new RuntimeException("Mã đoàn này đã được sử dụng");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setEmail(request.getEmail());
        tenant.setDomain(request.getDomain());
        tenant.setCategory(request.getCategory());
        tenant.setActive(true);
        // No notification-service yet (Phase 5) — activate immediately instead of an email-verification flow.
        tenant.setIsVerified(true);
        tenant.setStatusConfirm(RegistrationStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: role ADMIN chưa được seed"));

        User admin = User.builder()
                .username(request.getAdminUsername())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .email(request.getEmail())
                .fullName("Admin")
                .tenant(tenant)
                .roles(adminRole)
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .isVerified(true)
                .build();
        userRepository.save(admin);

        return TenantRegisterResponse.builder()
                .tenant(toResponse(tenant))
                .adminUsername(admin.getUsername())
                .build();
    }

    @Transactional
    public TenantResponse updateMyTheme(Long tenantId, UpdateTenantThemeRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị"));
        tenant.setPrimaryColorHex(request.getPrimaryColorHex());
        tenant.setAccentColorHex(request.getAccentColorHex());
        if (request.getProvince() != null) {
            tenant.setProvince(request.getProvince().trim());
        }
        if (request.getWard() != null) {
            String ward = request.getWard().trim();
            tenant.setWard(ward.isEmpty() ? null : ward);
        }
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream().map(this::toResponse).toList();
    }

    private TenantResponse toResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .domain(tenant.getDomain())
                .email(tenant.getEmail())
                .active(tenant.isActive())
                .isVerified(tenant.getIsVerified())
                .statusConfirm(tenant.getStatusConfirm())
                .category(tenant.getCategory())
                .province(tenant.getProvince())
                .ward(tenant.getWard())
                .primaryColorHex(tenant.getPrimaryColorHex())
                .accentColorHex(tenant.getAccentColorHex())
                .build();
    }
}
